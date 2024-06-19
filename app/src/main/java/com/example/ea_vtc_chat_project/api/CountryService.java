package com.example.ea_vtc_chat_project.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface CountryService {
    @GET("v3.1/all")
    Call<List<Country>> getAllCountries();
}