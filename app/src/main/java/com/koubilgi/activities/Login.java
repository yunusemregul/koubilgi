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

import com.koubilgi.MainApplication;
import com.koubilgi.R;
import com.koubilgi.api.Student;
import com.koubilgi.utils.ConnectionListener;

public class Login extends AppCompatActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		final Student student = Student.getInstance();
		// Eğer öğrenci daha önceden giriş yaptıysa otomatik giriş yap, ana menü activity sini başlat
		if (student.hasCredentials())
		{
			finish();
			Intent intent = new Intent(this, Mainmenu.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			startActivity(intent);
			overridePendingTransition(0, 0); // Kayma gibi bir animasyon oluyor onu engellemek için
			return;
		}

		setTheme(R.style.AppTheme);
		setContentView(R.layout.activity_login);

		// DP boyutunu öğren
		final DisplayMetrics metrics = getResources().getDisplayMetrics();

		final Button button = findViewById(R.id.button_login);
		button.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// öğrenci numarası ve şifresi girişleri
				EditText eStud = findViewById(R.id.entry_studentnumber), ePass = findViewById(R.id.entry_pass);

				final GradientDrawable gStudBackground = (GradientDrawable) eStud.getBackground(), gPassBackground = (GradientDrawable) ePass.getBackground();

				// TODO: 'Numara ve şifre girişleri doldurulmalıdır' hata ekranı gösterilmeli.
				// Eğer girişler doldurulmadıysa 2DP kırmızı çerçeve ekler
				// Kod tekrarını önlemek için bir yol bulunabilir belki kod çok kötü gözüküyor
				if (eStud.getText().length() == 0)
					gStudBackground.setStroke((int) metrics.density * 2, Color.RED);
				else
					gStudBackground.setStroke((int) metrics.density, getApplicationContext().getResources().getColor(R.color.colorBorders));

				if (ePass.getText().length() == 0)
					gPassBackground.setStroke((int) metrics.density * 2, Color.RED);
				else
					gPassBackground.setStroke((int) metrics.density, getApplicationContext().getResources().getColor(R.color.colorBorders));

				if (eStud.getText().length() > 0 && ePass.getText().length() > 0)
				{
					// Öğrencinin girdiği bilgilerle giriş yapmaya çalış
					final String numb = eStud.getText().toString(), pass = ePass.getText().toString();

					student.logIn(numb, pass, new ConnectionListener()
					{
						@Override
						public void onSuccess(String... args)
						{
							Intent intent = new Intent(getBaseContext(), Mainmenu.class);
							finish();
							startActivity(intent);
							overridePendingTransition(0, 0); // Kayma gibi bir animasyon oluyor onu engellemek için
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

	@Override
	protected void onResume()
	{
		super.onResume();
		MainApplication.setActiveActivity(this);
	}
}
