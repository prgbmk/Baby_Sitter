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
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


public class LogActivity extends Fragment {

    //커스텀된 ArrayList
    ArrayList<MysensorValue> sensorValues;
    MysensorValue myvalue;//센서값 담는 객체(커스텀된거) - 그림, 값.
    ListView currentSensorList;
    //어댑터 준비
    MysensorValueAdapter Adapter;
    DBGetService mService;
    boolean mBound = false;
    //UI 담는 객체.
    private Button getsensorBtn;
    private String DB_Result;
    //서비스 쓰기위한 객체 선언
    private Intent intent;//DBGetService와 연결할 intent객체
    private String URL;//Service에 보낼 PHP주소를 담는 String 객체

    //DBGetService의 리스너 호출
    private DBGetService.LogListener listener;

    //Service Binding - bindService()를 실행시 각종 세팅이 들어간다.
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DBGetService.LocalBinder binder = (DBGetService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            //mService.setContinueBoolean(true);

            listener = new DBGetService.LogListener() {
                @Override
                public void onLoggetted(String DB_Result) {//서비스 바인딩 하면 데이터가
                    //이 데이터를 가공해서 커스텀 리스트뷰에 뿌려줌.
                    setAdapter(DB_Result);
                }
            };
            mService.setOnLoggetted(listener);

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //xml와 객체 연결
        currentSensorList = (ListView)getActivity().findViewById(R.id.loglistView);

        //커스텀 리스트뷰용 객체 선언.
        sensorValues = new ArrayList<MysensorValue>();
        Adapter = new MysensorValueAdapter(getActivity(), R.layout.item, sensorValues);


        URL = "http://119.199.154.131/babysitter/allData_show(baby_sitter).php";
        //Service용 Intent연결.
        //1. Service의 바인딩과 연결하기 전에 세팅을 한 뒤에(URL주소)
        //2. 바인딩한 서비스를 시작한다.
        //이 서비스는 DBGetService.class 인데
        //이녀석이 DB데이터를 지속적으로 읽어오면서 LogActivity의 UI를 변경한다.
        intent = new Intent(getActivity(), DBGetService.class);//여기서 서비스를 쓰는 클라이언트는 이 클래스 자체가 된다.
        intent.putExtra("URL", URL);
        intent.putExtra("Type", 2);//LogActivity는 2로 지정.
        //getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        //getActivity().startService(intent);
    }


    @Override
    public void onStart() {
        super.onStart();
        //getActivity().startService(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mService != null) {
            Log.d("onPause2(LogActivity)", "호출됨");
            //화면 안보여질 때 서비스 false.. 서비스 Unbind!
            if(mBound){
                getActivity().unbindService(mConnection);
                mBound = false;
                //mService.setContinueBoolean(true);
                getActivity().stopService(intent);
            }
        }
    }

    //어댑터 리스트에 DB내용을 간추려서 넣는 메소드
    private void setAdapter(String DB_Result){
        String enterTok[] = null;
        enterTok = DB_Result.split("\n");
        int i = 1;
        sensorValues.clear();
        try{
            if(enterTok != null){
                while(i<enterTok.length){
                    String dataTok[] = enterTok[i].split("[*]");
                    myvalue = new MysensorValue(getBabyface(Integer.valueOf(dataTok[1]), Integer.valueOf(dataTok[2])),
                            calcTime(dataTok[3]) + " - " + "온도 : " + dataTok[1] + "°c, " + "습도 : "+ dataTok[2] + "%");
                    sensorValues.add(myvalue);
                    i++;
                }
                //Adapter.updateCensorList(sensorValues);
                currentSensorList.setAdapter(Adapter);
            }
        }
        catch(Exception e){
            Log.d("setAdapter Exception", e.toString());
        }
    }

    //온도 습도를 입력받아 적절한 아이콘값을 반해주는 메소드
    protected int getBabyface(int temp, int humi){
        if((temp >= 22 && temp <=24) && (humi >=45 && humi <=50))
            return R.drawable.good_nobg;
        else if((temp >=25 && temp <= 26) && (humi >= 51 && humi <= 60))
            return R.drawable.normal_nobg;
        else if((temp >=27 && temp <= 28) && (humi >= 61 && humi <= 70))
            return R.drawable.bad_nobg;
        else
            return R.drawable.verybad_nobg;
    }

    //시간 1055(AM 10:55) 형식으로 넘어오는거 해석해서 반환해주는 메소드
    private String calcTime(String timeChar){
        String hour;
        String min;
        hour = String.valueOf(timeChar.charAt(0)) + String.valueOf(timeChar.charAt(1));
        min = String.valueOf(timeChar.charAt(2)) + String.valueOf(timeChar.charAt(3));
        if(Integer.valueOf(hour)<12){
            return "AM " + hour + ":" + min;
        }
        else{
            return "PM " + hour + ":" + min;
        }
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        //mService.setContinueBoolean(true);
        if(!menuVisible) {//보이지 않는 상황에는 서비스 실행 X 및 기존 서비스 종료.
            if (mService != null) {
                if(mBound){
                    getActivity().unbindService(mConnection);
                    mBound = false;
                    mService.setContinueBoolean(true);
                }
            }
        }
        else{//보이게 되면 서비스 실행.
            getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            getActivity().startService(intent);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_log, container, false);

        return view;
    }



}

//리스트뷰 출력 항목 클래스
class MysensorValue{
    int Icon;
    String value;

    //기본 생성자.
    MysensorValue(int Icon, String value){
        this.Icon = Icon;
        this.value = value;
    }
}

//BaseAdapter 인터페이스를 오버라이드하여 커스텀 구현
class MysensorValueAdapter extends BaseAdapter {

    Context con;
    LayoutInflater inflater;
    ArrayList<MysensorValue> arD;
    int layout;

    //기본 생성자
    public MysensorValueAdapter(Context con, int layout, ArrayList<MysensorValue> arD){
        this.con = con;
        this.inflater = (LayoutInflater) con.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.arD = arD;
        this.layout = layout;
    }

    //어댑터 몇 개의 항목이 있는지 확인
    @Override
    public int getCount() {
        return arD.size();
    }

    //Position 위치의 항목 Value 반환
    @Override
    public Object getItem(int position) {
        return arD.get(position).value;
    }

    //Position 위치의 ID 반환
    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = inflater.inflate(layout, parent, false);
        }
        ImageView img = (ImageView) convertView.findViewById(R.id.img);
        img.setImageResource(arD.get(position).Icon);

        TextView txt = (TextView) convertView.findViewById(R.id.txt);
        txt.setText(arD.get(position).value);

        return convertView;
    }

    //서비스로 부터 들어오는 데이터 값을 새로 받아서 갱신하기 위해.
    public void updateCensorList(ArrayList<MysensorValue> newlist){
        arD.clear();
        arD.addAll(newlist);
        this.notifyDataSetChanged();
    }

}
