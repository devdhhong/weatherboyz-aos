package com.example.weatherboyz

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener

    private var latitude: Double? = null
    private var longitude: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // activity_main.xml을 로드

        // WebView 초기화
        webView = findViewById(R.id.webView)

        // WebViewClient 설정 (외부 브라우저로 리디렉션되지 않도록)
//        webView.webViewClient = WebViewClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // 위치 정보가 저장되어 있다면 JavaScript를 호출
                latitude?.let { lat ->
                    longitude?.let { lon ->
                        val jsCode = "javascript:receiveLocation($lat, $lon)"
                        webView.evaluateJavascript(jsCode) { result ->
                            Log.d("JS Result", result ?: "No result")
                        }
                    }
                }
            }
        }

        // JavaScript 사용 허용
        webView.settings.javaScriptEnabled = true

        // DOM Storage 활성화
        webView.settings.domStorageEnabled = true

        // 캐시 비활성화 (선택 사항)
        webView.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE

        // 자바스크립트 활성화
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true

        // JavaScript와의 통신을 위한 인터페이스 추가
        webView.addJavascriptInterface(AndroidInterface(), "Android")

        // LocationManager를 이용하여 위치 정보 수집
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                // 위치 정보를 저장
                latitude = location.latitude
                longitude = location.longitude

                // 위치 정보가 변경될 때마다 로그 출력 (확인용)
                Log.d("[JS] Location Changed", "Lat: $latitude, Lon: $longitude")
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                // 필요시 구현
            }

            override fun onProviderEnabled(provider: String) {
                // 필요시 구현
            }

            override fun onProviderDisabled(provider: String) {
                // 필요시 구현
            }
        }


        // 위치 권한 체크
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }


        // 위치 업데이트 요청
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, // 공급자
            2000, // 최소 시간 간격 (2초)
            10f, // 최소 거리 (10미터)
            locationListener // LocationListener
        )

        // 특정 URL 로드
        webView.loadUrl("https://weatherboyz.netlify.app/#/")
    }

    // Toast 메시지를 표시하는 메서드
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // 자바스크립트와 상호작용하기 위한 인터페이스 정의
    inner class AndroidInterface {
        @JavascriptInterface
        fun showToast(message: String) {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }

        @JavascriptInterface
        fun writeLog(title: String, message: String) {
            Log.d(title, message)
        }

        @JavascriptInterface
        fun getGPS(title: String, message: String) {
            Log.d(title, message)
        }

        @JavascriptInterface
        fun btnShare(message: String) {
            // 공유할 내용을 설정
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "$message") // 제목과 메시지를 공유
                type = "text/plain" // 공유할 데이터의 타입을 설정
            }

            // 공유할 앱을 선택할 수 있도록 Intent를 실행
            startActivity(Intent.createChooser(shareIntent, "테스트"))
        }

        @JavascriptInterface
        fun openOtherApp(appUrl: String, storeUrl: String) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(appUrl))

            try {
                startActivity(intent) // 특정 앱 열기
            } catch (e: ActivityNotFoundException) {
                // 앱이 설치되어 있지 않은 경우 앱스토어로 유도
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse(storeUrl))
                startActivity(marketIntent) // Play Store 페이지 열기
            }
        }
    }
}
