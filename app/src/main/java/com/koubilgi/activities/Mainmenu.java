package com.koubilgi.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.koubilgi.MainApplication;
import com.koubilgi.R;
import com.koubilgi.api.Student;
import com.koubilgi.components.SubmenuButtonAdapter;
import com.koubilgi.utils.AssetReader;
import com.koubilgi.utils.ConnectionListener;

import org.json.JSONObject;

// TODO: Duyurulara scroll atarken üst bar küçülmeli parallax gibi. Neye benzeyeceği tasarım hedeflerinde görülebilir.

public class Mainmenu extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_mainmenu);

        Student student = Student.getInstance();

        if (student == null || !student.hasCredentials()) {
            finish();
            Intent intent = new Intent(this, Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            overridePendingTransition(0, 0); // Kayma gibi bir animasyon oluyor onu engellemek için
            return;
        }

        final TextView tStudentName = findViewById(R.id.studentName);
        final TextView tStudentNumber = findViewById(R.id.studentNumber);
        final TextView tStudentDepartment = findViewById(R.id.studentDepartment);

        if (student.getName() != null) tStudentName.setText(student.getName());
        if (student.getNumber() != null) tStudentNumber.setText(student.getNumber());
        if (student.getDepartment() != null) tStudentDepartment.setText(student.getDepartment());
        else {
            student.getPersonalInfo("Bölüm", new ConnectionListener() {
                @Override
                public void onSuccess(String... args) {
                    String department = args[0];
                    if (department != null) {
                        tStudentDepartment.setText(department);
                    } else {
                        // TODO
                    }
                }

                @Override
                public void onFailure(String reason) {
                    // TODO
                }
            });
        }

        GridView submenus = findViewById(R.id.submenus);
        submenus.setAdapter(new SubmenuButtonAdapter(this));

        String departmentDuyurularUrlAndTitles = "";
        JSONObject duyurularUrlAndTitles;
        try {
            departmentDuyurularUrlAndTitles = AssetReader.readFileAsString("departmentDuyurularUrlAndTitles.txt");
            duyurularUrlAndTitles = new JSONObject(departmentDuyurularUrlAndTitles);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        MainApplication.setActiveActivity(this);
    }

    public void openSettingsSubmenu(View view) {
        // TODO
    }

    public void openMessagesSubmenu(View view) {
        // TODO
    }
}
