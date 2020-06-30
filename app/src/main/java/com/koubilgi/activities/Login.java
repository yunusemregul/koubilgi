package com.koubilgi.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.koubilgi.R;
import com.koubilgi.api.Student;
import com.koubilgi.utils.ConnectionListener;

public class Login extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        final Student student = Student.getInstance(this);
        // Start the main menu activity if user has logged in before
        if (student.hasCredentials())
        {
            finish();
            Intent intent = new Intent(this, Mainmenu.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            overridePendingTransition(0, 0); // Avoid sliding animation
            return;
        }

        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_login);

        // Get relative DP size
        final DisplayMetrics metrics = getResources().getDisplayMetrics();

        final Button button = findViewById(R.id.button_login);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // entry StudentNumber, entry Pass
                EditText eStud = findViewById(R.id.entry_studentnumber), ePass = findViewById(R.id.entry_pass);

                final GradientDrawable gStudBackground = (GradientDrawable) eStud.getBackground(), gPassBackground =
                        (GradientDrawable) ePass.getBackground();

                // TODO: 'Numara ve şifre girişleri doldurulmalıdır' hata ekranı gösterilmeli.
                // Set red stroke of 2dp when there's no text on entries
                // Should find some better way to not repeat the code
                if (eStud.getText().length() == 0)
                    gStudBackground.setStroke((int) metrics.density * 2, Color.RED);
                else
                    gStudBackground.setStroke((int) metrics.density,
                            getApplicationContext().getResources().getColor(R.color.colorBorders));

                if (ePass.getText().length() == 0)
                    gPassBackground.setStroke((int) metrics.density * 2, Color.RED);
                else
                    gPassBackground.setStroke((int) metrics.density,
                            getApplicationContext().getResources().getColor(R.color.colorBorders));

                if (eStud.getText().length() > 0 && ePass.getText().length() > 0)
                {
                    // Login to the site with student credentials
                    final String numb = eStud.getText().toString(), pass = ePass.getText().toString();

                    student.logIn(numb, pass, new ConnectionListener()
                    {
                        @Override
                        public void onSuccess(String... args)
                        {
                            Intent intent = new Intent(getBaseContext(), Mainmenu.class);
                            finish();
                            startActivity(intent);
                            overridePendingTransition(0, 0); // Avoid sliding animation
                        }

                        @Override
                        public void onFailure(String reason)
                        {
                            if (reason.equals("credentials"))
                            {
                                // TODO: 'Öğrenci numarası ya da şifresi hatalı' hata ekranı gösterilmeli.
                                gStudBackground.setStroke((int) metrics.density * 2, Color.RED);
                                gPassBackground.setStroke((int) metrics.density * 2, Color.RED);
                                return;
                            }
                            // TODO: Eğer giriş yapılamamasının sebebi site ise 'Site bozuk' gibi bir hata gösterilmeli.
                        }
                    });
                }
            }
        });
    }
}
