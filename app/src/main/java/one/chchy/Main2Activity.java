package one.chchy;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import org.libreoffice.MainOffice;

public class Main2Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.maintoolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.mainfab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        Log.d("Main2Activity", "runtime = " + Runtime.getRuntime().maxMemory() + ", memory = " + activityManager.getMemoryClass());
//        MainOffice.Instance(this).Load("/sdcard/docx4j.doc");
//        MainOffice.Instance(this).Load("/sdcard/打印机接口文档.docx");
//        MainOffice.Instance(this).Load("/sdcard/LOREM IPSUM DOL.pptx");
//        MainOffice.In+stance(this).onStart();
//        MainOffice.Instance(this).Load("/sdcard/excel.xls");
        MainOffice.Instance(this).Load("/sdcard/jiatingyusuan.xlsx");
        MainOffice.Instance(this).onStartSize(1488, 2104);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MainOffice.Instance(this).onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainOffice.Instance(this).onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
//        MainOffice.Instance(this).onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        MainOffice.Instance(this).onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MainOffice.Instance(this).onDestroy();
    }
}
