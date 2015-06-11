package com.test.mungipark.baby_sitter;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by MungiPark on 2015. 6. 4..
 * DB정보를 받아와 결과를 반환해줄 서비스 클래스
 * 필요할 때마다 PHP 경로를 바꿔서 원하는 DB 데이터 녀석을 가져올 수 있다.
 */

public class DBGetService extends Service {
    private String URL;
    private String DB_Result;//DB 결과...

    private boolean mbound = true;//스레드 계속할 지 판단하는 메소드
    //1. MainAcitvity.java
    //2. LogActivity.java
    private int type;//클라이언트가 어느녀석인지 판단
    //Type에 따라 다른 리스너를 붙여주기 위해서.

    //private AsyncTask task;

    //DB정보를 받으면 UI를 업데이트 하기 위해 리스너를 하나 새로 정의
    private Listener myListener;//MainActivity용 리스너
    private LogListener myListener2;//DBGetService용 리스너

    //MainActivity용 리스너
    public interface Listener{
        public void onDBgetted(String DB_Result);
    }

    public void setOnDBgetted(Listener listener){
        this.myListener = listener;
    }
    public void onDBgetted(String str){ }
    //DB리스너 정의 끝 - for MainActivity.java

    //LogActivity용
    public interface LogListener{
        public void onLoggetted(String DB_Result);
    }
    public void setOnLoggetted(LogListener listener){
        this.myListener2 = listener;
    }
    public void onLoggetted(String str){ }
    //DB리스너 정의 끝 - for LogActivity.java




    //이 클래스를 생성할 생성자
    public DBGetService() {

    }
    //로컬 바인더 정의
    private final IBinder mBinder = new LocalBinder();

    //이 메소드를 호출하면 클래스 자체를 보내주고 이 클래스의 내용을 접근 할 수 있게 된다.
    public class LocalBinder extends Binder{
        DBGetService getService() {
            return DBGetService.this;
        }
    }

    //이 서비스 클래스가 시작 될 때 실행할 작업.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //mbound = true;
        if(intent != null){
            URL = intent.getStringExtra("URL");
            type = intent.getIntExtra("Type", 1);//어떤 클래스가 서비스 호출했는지 판단
            new showDB().execute();
        }
        Log.d("Service 실행", "실행됨");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //mbound = false;
        /*
        mbound = false;
        task.cancel(true);
        Log.d("onDestroy(Unbind)", "호출");
        */
    }

    //저장된 센서정보 보는 메소드 돌리는 스레드
    private class showDB extends AsyncTask<Void, String, Void> {


        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected Void doInBackground(Void... params) {

            String output;
            Log.d("DBGetService", URL);
            switch(type){
                case 1://MainActivity는 계속 갱신해야하므로 1초마다 갱신
                    while(mbound){
                        output = ShowData();
                        publishProgress(output);//onProgressUpdate에 인자 넘겨줌
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                    break;
                case 2://LogActivity는 한번하고 끝.
                    output = ShowData();
                    publishProgress(output);//onProgressUpdate에 인자 넘겨줌
                    break;
                default:
                    break;
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            switch(type){
                case 1:
                    if (myListener != null) {//리스너 작동
                        myListener.onDBgetted(values[0]);
                    }
                    break;
                case 2:
                    if(myListener2 != null){
                        myListener2.onLoggetted(values[0]);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    //DB데이터 읽는 메소드 - AsyncTask 스레드에 탐재할 함수
    private String ShowData() {
        java.net.URL url = null;
        try {
            //url = new URL("http://119.199.154.131/babysitter/allData_show(baby_sitter).php");
            url = new URL(URL);

            HttpURLConnection http = (HttpURLConnection) url.openConnection();//php접속

            http.setDefaultUseCaches(false);
            http.setDoInput(true);//서버 읽기 모드
            http.setDoOutput(true);//서버 쓰기 모드
            http.setRequestMethod("POST");//POST방식 전송(보안용)
            http.setRequestProperty("content-type", "application/x-www-form-urlencoded");

            //파라미터값 넘기고나서 나오는 결과 받기
            InputStreamReader tmp = new InputStreamReader(http.getInputStream(), "UTF-8");
            BufferedReader reader = new BufferedReader(tmp);
            StringBuilder builder = new StringBuilder();
            String str;
            while ((str = reader.readLine()) != null) {
                builder.append(str + "\n");
            }
            DB_Result = builder.toString();
            Log.d("DB_Result : ", DB_Result);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.d("Show_data Exception: ", e.toString());
        }
        return DB_Result;
    }


    public String getDBResult(){
        return DB_Result;
    }

    //AsyncTask를 계속 돌릴지 판단할때 참고할 Boolean을 세팅할 메소드.
    public void setContinueBoolean(boolean threadout){
        mbound = threadout;
    }

    //다른 클라이언트가 이 서비스를 쓰고 있는지 판단하는 용도용도
  public boolean getmBound(){
        return mbound;
    }

    //이 서비스의 메소드 및 스레드를 쓰기위해 이 인텐드를 넘겨준다.
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("Service ", "Unbind");
        Log.d("Type ", String.valueOf(type));
        return super.onUnbind(intent);
    }


}
