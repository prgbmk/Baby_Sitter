package com.test.mungipark.baby_sitter;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

import com.mungi.customProgressbar.CustomProgressBar;
import com.mungi.customProgressbar.OnProgressBarListener;

public class MainActivity extends Fragment {

    DBGetService mService;
    boolean mBound = false;
    //UI객체들
    private ImageView babyimg;
    private CustomProgressBar TempBar, HumiBar;
    private TabHost tab;
    private TextView requireTxt;
    //서비스 쓰기위한 객체 선언
    private Intent intent;//DBGetService와 연결할 intent객체
    private String URL;//Service에 보낼 PHP주소를 담는 String 객체

    //DBGetService의 리스너 호출
    private DBGetService.Listener listener;

    private String DB_Result;

    //Service Binding - bindService()를 실행시 각종 세팅이 들어간다.
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DBGetService.LocalBinder binder = (DBGetService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.setContinueBoolean(true);

            listener = new DBGetService.Listener() {
                @Override
                public void onDBgetted(String DB_Result) {
                    setDBResult(DB_Result);

                }
            };
            mService.setOnDBgetted(listener);

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    //DB 결과 처리해서 알맞은 프로그레스 바 값으로 표시해줌
    //애기 얼굴 이미지 값도 반환해줌.
    private void setDBResult(String str){
        String enterTok[] = str.split("\n");
        String starTok[] = enterTok[1].split("[*]");

        float temp;
        float temp2 = Integer.valueOf(starTok[1]);
        temp = temp2 / 2;
        //temp = (float)35/2;

        //TempBar.setProgress(temp);
        TempBar.setProgress(temp);
        temp = Integer.valueOf(starTok[2]);
        HumiBar.setProgress(temp);
        babyimg.setImageResource(getBabyface(Integer.valueOf(starTok[1]), Integer.valueOf(starTok[2])));



    }

    //온도 습도를 입력받아 적절한 아이콘값을 반해주는 메소드
    //글자도 세팅.
    protected int getBabyface(int temp, int humi){
        if((temp >= 22 && temp <=24) && (humi >=45 && humi <=50)) {
            requireTxt.setText("가장 좋은 조건입니다. 지금 상태를 유지하세요");
            return R.drawable.good_nobg;
        }
        else if((temp >=25 && temp <= 26) && (humi >= 51 && humi <= 60)) {
            requireTxt.setText("온도가 조금 높지만 괜찮습니다. 에어콘을 켜는게 좋습니다.");
            return R.drawable.normal_nobg;
        }
        else if((temp >=27 && temp <= 28) && (humi >= 61 && humi <= 70)) {
            requireTxt.setText("온도와 습도가 높습니다. 에어콘을 켜고 온도와 습도를 내리세요.");
            return R.drawable.bad_nobg;
        }
        else {
            requireTxt.setText("온도와 습도가 너무 높습니다!, 에어콘을 켜고 온도와 습도를 내리세요!");
            return R.drawable.verybad_nobg;
        }
    }


    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if(!menuVisible){
            if(mService!=null)
                mService.setContinueBoolean(false);
        }
        else{
            if(mService != null){
                mService.setContinueBoolean(true);
                getActivity().startService(intent);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_main, container, false);

        return view;
    }

    //여기서 실제로 XML과 객체를 연결하는 작업을 해야한다.
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        TempBar = (CustomProgressBar)getActivity().findViewById(R.id.temp_progress_bar);
        HumiBar = (CustomProgressBar)getActivity().findViewById(R.id.humi_progress_bar);
        babyimg = (ImageView)getActivity().findViewById(R.id.babyImg);
        requireTxt = (TextView)getActivity().findViewById(R.id.requireTxt);

        TempBar.setMax(50);
        TempBar.setSuffix("°c");

        //Service용 Intent연결.
        //1. Service의 바인딩과 연결하기 전에 세팅을 한 뒤에(URL주소)
        //2. 바인딩한 서비스를 시작한다.
        //이 서비스는 DBGetService.class 인데
        //이녀석이 DB데이터를 지속적으로 읽어오면서 MainActivity의 UI를 변경한다.
        intent = new Intent(getActivity(), DBGetService.class);//여기서 서비스를 쓰는 클라이언트는 이 클래스 자체가 된다.
        //URL 정보 넘겨주고 Service시작
        URL = "http://119.199.154.131/babysitter/show_data(baby_sitter).php";
        intent.putExtra("URL", URL);
        intent.putExtra("Type", 1);//MainActivity는 1로 지정.
        //intent.putExtra("stopReading", true);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

    }


    @Override
    public void onStart() {
        super.onStart();
        getActivity().startService(intent);
        if(mService!=null){
            mService.setContinueBoolean(true);
        }
    }

    //Activity가 다시 재개 될 때.
    @Override
    public void onResume() {
        super.onResume();
        Log.d("onResume(MainActivity)", "호출됨");
        if(mService != null) {
            mService.setContinueBoolean(true);//서비스의 스레드를 계속 돌리라고 명시해줌
            getActivity().startService(intent);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("onPause(MainActivity)", "호출됨");
        if(mService!=null)
            mService.setContinueBoolean(false);
        getActivity().stopService(intent);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
