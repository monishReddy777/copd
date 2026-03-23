package com.simats.cdss.network;

import android.content.Context;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.simats.cdss.SessionManager;
import java.io.IOException;

public class RetrofitClient {
    private static final String BASE_URL = "https://3dxz8qrz-8000.inc1.devtunnels.ms/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request originalRequest = chain.request();
                        
                        // Skip token for auth endpoints
                        String path = originalRequest.url().encodedPath();
                        if (path.contains("/auth/") || path.contains("/register/")
                                || path.contains("/login/") || path.contains("/signup/")
                                || path.contains("/verify-otp/") || path.contains("/forgot-password/")
                                || path.contains("/reset-password/")) {
                            return chain.proceed(originalRequest);
                        }

                        SessionManager session = new SessionManager(context);
                        String token = session.getAccessToken();

                        if (token != null) {
                            Request newRequest = originalRequest.newBuilder()
                                    .header("Authorization", "Bearer " + token)
                                    .build();
                            return chain.proceed(newRequest);
                        }
                        return chain.proceed(originalRequest);
                    }
                }).build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }
}