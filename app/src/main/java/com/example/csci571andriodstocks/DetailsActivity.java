package com.example.csci571andriodstocks;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;

public class DetailsActivity extends AppCompatActivity {

    public static final String PRICE_URL = "https://csci571-trading-platform.wl.r.appspot.com/api/price/";
    public static final String DETAIL_URL = "https://csci571-trading-platform.wl.r.appspot.com/api/detail/";
    public static final String CHART_URL = "https://csci571-trading-platform.wl.r.appspot.com/api/chart/historical/";
    public static final String NEWS_URL = "https://csci571-trading-platform.wl.r.appspot.com/api/news/";
    public static final int TOT_API_CALLS = 3;

    private Map<String, String> myFavourites;
    private Map<String, String> myPortfolio;
    private boolean isFavourites;
    private String ticker;
    private String name;
    private double lastPrice;
    private Context ctx;
    private WebView wv;
    private GridView statGrid;
    private RecyclerView recyclerViewNews;
    private String[] stats;
    private View spinnerContainer;
    private int numApiCalls;
    private boolean isApiFailed;
    private CustomGridAdapter customGridAdapter;
    private CustomNewsAdapter customNewsAdapter;
    private NestedScrollView nestedScrollView;
    private List<News> newsList;
    private String cashInHand;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        isFavourites = false;
        Intent intent = getIntent();
        ticker = intent.getStringExtra(MainActivity.EXTRA_TICKER);
        ctx = this;
        numApiCalls = 0;
        isApiFailed = false;

        sharedPreferences = getSharedPreferences(LocalStorage.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        myPortfolio = LocalStorage.getFromStorage(LocalStorage.PORTFOLIO);

        wv = (WebView) findViewById(R.id.webView_chart);
        wv.loadUrl("file:///android_asset/charts.html");
        wv.getSettings().setJavaScriptEnabled(true);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        statGrid = findViewById(R.id.grid_view); // init GridView
        spinnerContainer = findViewById(R.id.progressbar_container);
        stats = new String[7];
        // Create an object of CustomAdapter and set Adapter to GirdView
        customGridAdapter = new CustomGridAdapter(this, stats);
        statGrid.setAdapter(customGridAdapter);

        nestedScrollView = findViewById(R.id.details_screen);
        recyclerViewNews = findViewById(R.id.rvNews);
        recyclerViewNews.setLayoutManager(new LinearLayoutManager(this));

        newsList = new ArrayList<>();
        customNewsAdapter = new CustomNewsAdapter(newsList, this,
                (newsItem, position) -> {

                    openChrome(newsItem.url);
                    Log.e("CLICK_NEWS", "news item clicked at " + position);
                },

                (newsItem, position) -> {

                    final Dialog dialog = new Dialog(ctx);
                    dialog.setContentView(R.layout.news_dialog);

                    TextView tvNewsTitle = dialog.findViewById(R.id.tvDialog_news_title);
                    ImageView ivNewsImg = dialog.findViewById(R.id.ivDialog_news_img);
                    ImageButton btnTwitter = dialog.findViewById(R.id.btn_twitter);
                    ImageButton btnChrome = dialog.findViewById(R.id.btn_chrome);

                    tvNewsTitle.setText(newsItem.title);

                    Picasso.with(ctx).load(newsItem.img).resize(1300, 0).into(ivNewsImg);

                    btnChrome.setOnClickListener(v -> openChrome(newsItem.url));
                    btnTwitter.setOnClickListener(v -> {
                        String url = "https://twitter.com/intent/tweet?text=Check out this link:" +
                                "&url=" + newsItem.url + "&hashtags=CSCI571StockApp";
                        openChrome(url);
                    });

                    dialog.show();
                    Log.e("LONGCLICK_NEWS", "news item clicked at " + position);
                }
        );
        recyclerViewNews.setAdapter(customNewsAdapter);


        wv.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url.equals("file:///android_asset/charts.html")) {
                    Log.i("CHART", "chart................................");
                    makeApiCallChart(ticker);
                }
            }
        });


        makeApiCallPrice(ticker);
        makeApiCallSummary(ticker);
        makeApiCallNews(ticker);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.details_menu, menu);
        myFavourites = LocalStorage.getFromStorage(LocalStorage.FAVOURITES);
        Log.i("fav", "favouroites" + myFavourites);


        MenuItem item = menu.findItem(R.id.toggle_star);
        Log.i("oncreatemeu", "...............item: " + ticker);

        if (myFavourites.containsKey(ticker)){
            item.setIcon(R.drawable.ic_baseline_star_24);
            isFavourites = true;
        } else{
            item.setIcon(R.drawable.ic_baseline_star_border_24);
            isFavourites = false;
        }

        return true;
    }

     // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                // User chose the "Settings" item, show the app settings UI...
                return true;

            case R.id.toggle_star:
                // User chose the "Favorite" action, mark the current item
                // as a favorite...

                myFavourites = LocalStorage.getFromStorage(LocalStorage.FAVOURITES);

                // do something here
                if (isFavourites){
                    isFavourites = false;
                    item.setIcon(R.drawable.ic_baseline_star_border_24);
                    myFavourites.remove(ticker);
                    Toast toast = Toast.makeText(this, ticker + " was removed from favourites", Toast.LENGTH_SHORT);
                    toast.show();
                } else{
                    isFavourites = true;
                    item.setIcon(R.drawable.ic_baseline_star_24);
                    myFavourites.put(ticker, name);
                    Toast toast = Toast.makeText(this, ticker + " was added to favourites", Toast.LENGTH_SHORT);
                    toast.show();
                }
                Log.i("added-removed", "favouroites......" + myFavourites);
                LocalStorage.setMap(LocalStorage.FAVOURITES, myFavourites);


                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }


    private void makeApiCallPrice(String ticker) {

        ApiCall.make(this, ticker, PRICE_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                try {
                    JSONObject responseObject = new JSONObject(response);
                    JSONArray array = responseObject.getJSONArray("results");
                    Log.i("length:", "len: " + array.length());
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject row = array.getJSONObject(i);
                        String last = (row.getString("last") != "null") ? row.getString("last") : row.getString("tngoLast");;
                        String prevClose = (row.getString("prevClose") != "null") ? row.getString("prevClose"): "0.0";
                        String low = (row.getString("low") != "null") ? row.getString("low"): "0.0";
                        String bidPrice = (row.getString("bidPrice") != "null") ? row.getString("bidPrice"): "0.0";;
                        String openPrice = (row.getString("open") != "null") ? row.getString("open"): "0.0";;
                        String mid = (row.getString("mid") != "null") ? row.getString("mid"): "0.0";
                        String high = (row.getString("high") != "null") ? row.getString("high"): "0.0";
                        String volume = (row.getString("volume") != "null") ? row.getString("volume"): "0.0";

                        TextView tvTicker = (TextView) findViewById(R.id.detail_ticker);
                        TextView tvLast = (TextView) findViewById(R.id.detail_last);
                        TextView tvChange = (TextView) findViewById(R.id.detail_change);
                        TextView tvMarketValue = findViewById(R.id.tvMarket_value);
                        TextView tvShares = findViewById(R.id.tvDetails_shares);

                        Locale locale = new Locale("en", "US");
                        NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);

                        Company company = new Company(name, ticker,"", last, prevClose, "", ctx);
                        lastPrice = Double.parseDouble(company.last);

                        company.last = fmt.format(Double.parseDouble(company.last));

                        tvTicker.setText(company.ticker);
                        tvChange.setText(fmt.format(company.change));
                        tvChange.setTextColor(company.changeColor);
                        tvLast.setText(company.last);
                        tvShares.setText("You have 0 shares of " + ticker);

                        myPortfolio = LocalStorage.getFromStorage(LocalStorage.PORTFOLIO);

                        if (myPortfolio.containsKey(ticker)){

                            String shares = myPortfolio.get(ticker);
                            double val = Double.parseDouble(shares) * Double.parseDouble(last);
                            tvShares.setText("Shares owned: " + shares);
                            tvMarketValue.setText("Market Value: " + fmt.format(val));
                        }else {
                            tvMarketValue.setText("Start Trading|");
                        }


                        String[] newSats = {
                                "Current Price: " + last,
                                "Low: " + low,
                                "Bid Price: " + bidPrice,
                                "Open Price: " + openPrice,
                                "Mid: " + mid,
                                "High: " + high,
                                "Volume: " + volume
                        };

                        customGridAdapter.setData(newSats);
                        customGridAdapter.notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    isApiFailed = true;
                }

                numApiCalls++;

                if (isApiFailed){
                    numApiCalls = 0;
                    // display an error message or do some error handling

                } else if (numApiCalls == TOT_API_CALLS){
                    numApiCalls = 0;
                    spinnerContainer.setVisibility(View.GONE);
                    nestedScrollView.setVisibility(View.VISIBLE);
                    isEllipses();
                }

            }
        }, (Response.ErrorListener) error -> {
            isApiFailed = true;
            Log.i("error", "error in search http " + error);
        });
    }

    private void makeApiCallSummary(String ticker) {

        ApiCall.make(this, ticker, DETAIL_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                try {
                    JSONObject responseObject = new JSONObject(response);
                    JSONArray array = responseObject.getJSONArray("results");
                    Log.i("length:", "len: " + array.length());
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject row = array.getJSONObject(i);
                        String description = row.getString("description");
                        name = row.getString("name");;

                        TextView tvCompanyName = (TextView) findViewById(R.id.detail_company_name);
                        TextView tvDescription = (TextView) findViewById(R.id.tvDetails_desc);
                        tvCompanyName.setText(name);
                        tvDescription.setText(description);

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                numApiCalls++;

                if (isApiFailed){
                    numApiCalls = 0;
                    // display an error message or do some error handling

                } else if (numApiCalls == TOT_API_CALLS){
                    numApiCalls = 0;
                    spinnerContainer.setVisibility(View.GONE);
                    nestedScrollView.setVisibility(View.VISIBLE);
                    isEllipses();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                isApiFailed = true;
                Log.i("error", "error in search http " + error);
            }
        });
    }

    private void openChrome(String url){
        Uri uri = Uri.parse(url); // missing 'http://' will cause crashed
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);

    }


    public void openSuccessDialog(double sharesTraded, String type) {

        TextView tvMarketValue = findViewById(R.id.tvMarket_value);
        TextView tvShares = findViewById(R.id.tvDetails_shares);
        Locale locale = new Locale("en", "US");
        NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);

        final Dialog dialog = new Dialog(ctx);
        dialog.setContentView(R.layout.success_trade_dialog);

        TextView tvTradedMsg = (TextView) dialog.findViewById(R.id.tv_shares_traded_msg);
        tvTradedMsg.setText("You have successfully " + type + " " + sharesTraded + " of " + ticker);
        Button btnDone = (Button) dialog.findViewById(R.id.btn_done);

        dialog.show();

        btnDone.setOnClickListener(v -> {
            dialog.dismiss();
            numApiCalls = TOT_API_CALLS - 1;

        });

        tvShares.setText("You have 0 shares of " + ticker);

        myPortfolio = LocalStorage.getFromStorage(LocalStorage.PORTFOLIO);

        if (myPortfolio.containsKey(ticker)){

            String shares = myPortfolio.get(ticker);
            double val = Double.parseDouble(shares) * lastPrice;
            tvShares.setText("Shares owned: " + shares);
            tvMarketValue.setText("Market Value: " + fmt.format(val));
        }else {
            tvMarketValue.setText("Start Trading|");
        }

        makeApiCallPrice(ticker);

    }


    public void openTradeDialog(View view) {

        final Dialog dialog = new Dialog(ctx);
        dialog.setContentView(R.layout.trading_dialog);

        TextView tvTitle = (TextView) dialog.findViewById(R.id.tv_trading_title);
        TextView tvTotVal = (TextView) dialog.findViewById(R.id.tv_trading_tot_share_val);
        TextView tvCashInHand = (TextView) dialog.findViewById(R.id.tv_trading_cash);
        EditText etShareInput = (EditText) dialog.findViewById(R.id.et_dialog_input_shares);
        Button btnBuy = (Button) dialog.findViewById(R.id.btn_buy);
        Button btnSell = (Button) dialog.findViewById(R.id.btn_sell);

        cashInHand = sharedPreferences.getString(LocalStorage.CASH_IN_HAND, "20000.00");


        tvTitle.setText("Trade " + name + " shares");
        tvTotVal.setText("0 x " + lastPrice + "/share = " + "$0.00");
        tvCashInHand.setText("$" + cashInHand + " available to buy " + ticker);


        btnBuy.setOnClickListener(v -> {

            double sharesInputed = 0;
            double cashUsed;

            if (etShareInput.getText().length() != 0){
                try {
                    sharesInputed = Double.parseDouble(String.valueOf(etShareInput.getText()));
                } catch(NumberFormatException e){
                    sharesInputed = 0;
                    Toast toast = Toast.makeText(ctx, "Please enter valid amount", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
            }

            if (sharesInputed <= 0){

                Toast toast = Toast.makeText(this, "Cannot buy less than 0 shares", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }

            if (lastPrice * sharesInputed > Double.parseDouble(cashInHand)){

                Toast toast = Toast.makeText(this, "Not enough money to buy", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }

            if (myPortfolio.containsKey(ticker)){

                double totShares = Double.parseDouble(myPortfolio.get(ticker)) + sharesInputed;
                myPortfolio.put(ticker, String.valueOf(totShares));

            }else{
                myPortfolio.put(ticker, String.valueOf(sharesInputed));
            }

            LocalStorage.setMap(LocalStorage.PORTFOLIO, myPortfolio);
            cashUsed = (lastPrice * sharesInputed);
            DecimalFormat df = new DecimalFormat("####0.00");
            cashInHand = df.format(Double.parseDouble(cashInHand) - cashUsed);
            editor.putString(LocalStorage.CASH_IN_HAND, cashInHand);
            editor.commit();
            dialog.dismiss();

            openSuccessDialog(sharesInputed, "bought");

        });

        btnSell.setOnClickListener(v -> {

            double sharesInputed = 0;
            double cashReceived;

            if (etShareInput.getText().length() != 0) {

                try {
                    sharesInputed = Double.parseDouble(String.valueOf(etShareInput.getText()));
                } catch (NumberFormatException e) {
                    Toast toast = Toast.makeText(ctx, "Please enter valid amount", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
            }


            if (sharesInputed <= 0) {

                Toast toast = Toast.makeText(this, "Cannot sell less than 0 shares", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }

            if (!myPortfolio.containsKey(ticker) || Double.parseDouble(myPortfolio.get(ticker)) < sharesInputed) {
                Toast toast = Toast.makeText(this, "Not enough shares to sell", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }

            if (BuildConfig.DEBUG && !myPortfolio.containsKey(ticker)) {
                throw new AssertionError("Assertion failed");
            }


            double totShares = Double.parseDouble(myPortfolio.get(ticker)) - sharesInputed;
            DecimalFormat df = new DecimalFormat("####0.00");


            Log.e("outside", "out: " + myPortfolio.get(ticker) + " " + etShareInput.getText());
            if (totShares <= 0.000001){
                Log.e("here", "here");
                myPortfolio.remove(ticker);
            }else{
                myPortfolio.put(ticker, String.valueOf(df.format(totShares)));
            }

            LocalStorage.setMap(LocalStorage.PORTFOLIO, myPortfolio);
            cashReceived = (lastPrice * sharesInputed);

            cashInHand = df.format(Double.parseDouble(cashInHand) + cashReceived);
            editor.putString(LocalStorage.CASH_IN_HAND, cashInHand);
            editor.commit();
            dialog.dismiss();

            openSuccessDialog(sharesInputed, "sold");

        });


        etShareInput.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {

                double shares = 0;
                double totVal;
                String totValTxt;
                Locale locale = new Locale("en", "US");
                NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);
                String lastStr;
                String totValStr;

                if (s.length() == 0){
                    s = "0";
                }else{

                    try {
                        shares = Double.parseDouble(s.toString());
                    } catch(NumberFormatException e){
                        shares = 0;
                    }
                }

                totVal = lastPrice * shares;
                lastStr = fmt.format(lastPrice);
                totValStr = fmt.format(totVal);

                totValTxt = s + " x " + lastStr + "/share = " + totValStr;
                tvTotVal.setText(totValTxt);

            }
        });

        dialog.show();

    }
    public void toggleDescription(View view){

        TextView tvDescription = (TextView) findViewById(R.id.tvDetails_desc);
        Button toggleDescBtn = (Button) findViewById(R.id.btn_desc);

        CharSequence btnDescTxt = toggleDescBtn.getText();

        Log.d("button", ".........button pressed text: " + btnDescTxt);

        if (btnDescTxt.equals("Show more...")){
            toggleDescBtn.setText("Show less");
            tvDescription.setMaxLines(Integer.MAX_VALUE);
            tvDescription.setEllipsize(null);

        }else{
            toggleDescBtn.setText("Show more...");
            tvDescription.setMaxLines(2);
            tvDescription.setEllipsize(TextUtils.TruncateAt.END);
        }
    }

    public void isEllipses(){

        TextView tvDescription = (TextView) findViewById(R.id.tvDetails_desc);
        Button toggleDescBtn = (Button) findViewById(R.id.btn_desc);

        tvDescription.post(new Runnable() {
            @Override
            public void run() {
                int lines = tvDescription.getLineCount();
                if (lines > 2) {
                    //do something
                    tvDescription.setMaxLines(2);
                    tvDescription.setEllipsize(TextUtils.TruncateAt.END);
                    toggleDescBtn.setVisibility(View.VISIBLE);
                    Log.d("LINES", "..........There are " + lines);
                }
            }
        });

    }

    private void makeApiCallNews(String ticker) {

        ApiCall.make(this, ticker, NEWS_URL, new Response.Listener<String>() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onResponse(String response) {
                //parsing logic, please change it as per your requirement
                List<News> tempNewsList = new ArrayList<>();

                try {
                    JSONObject responseObject = new JSONObject(response);
                    JSONArray array = responseObject.getJSONArray("results");
                    Log.i("length:", "len: " + array.length());
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject row = array.getJSONObject(i);
                        String url = row.getString("url");
                        String title = row.getString("title");
                        String src = row.getString("source");
                        String urlToImg = row.getString("urlToImage");
                        String timestamp = row.getString("publishedAt");

                        if (i == 10){
                            Log.i("NEWS", "urlToImg: " + urlToImg + "\ntitle: " + title);

                        }


                        News newsItem = new News(src, urlToImg, title, timestamp, url);
                        tempNewsList.add(newsItem);

                    }
                } catch (Exception e) {
                    isApiFailed = true;
                    e.printStackTrace();
                }

                newsList.clear();
                newsList.addAll(tempNewsList);
                customGridAdapter.notifyDataSetChanged();

                numApiCalls++;

                if (isApiFailed){
                    numApiCalls = 0;
                    // display an error message or do some error handling

                } else if (numApiCalls == TOT_API_CALLS){
                    numApiCalls = 0;
                    spinnerContainer.setVisibility(View.GONE);
                    nestedScrollView.setVisibility(View.VISIBLE);
                    isEllipses();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                isApiFailed = true;
                Log.i("error", "error in search http " + error);
            }
        });
    }


    private void makeApiCallChart(String ticker) {

        ApiCall.make(this, ticker, CHART_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                try {
                    JSONObject responseObject = new JSONObject(response);
                    JSONArray array = responseObject.getJSONArray("results");
                    Log.i("length:", "len: " + array.length());
                    wv.loadUrl("javascript:createChart('"+ticker+"', '"+array+"');");

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }, error -> Log.i("error", "error in search http " + error));
    }



}