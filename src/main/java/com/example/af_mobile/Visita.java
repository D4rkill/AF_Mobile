package com.example.af_mobile;

import com.google.firebase.firestore.Exclude;

public class Visita {

    private String id;
    private String titulo;
    private String descricao;
    private String data;
    private String categoria;
    private boolean favorito;
    private boolean memoravel;
    private double latitude;
    private double longitude;
    private String temperatura;
    private String vento;
    private String condicao;

    public Visita() {
    }

    public Visita(String titulo, String descricao, String data, String categoria,
                  boolean favorito, boolean memoravel,
                  double latitude, double longitude,
                  String temperatura, String vento, String condicao) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.data = data;
        this.categoria = categoria;
        this.favorito = favorito;
        this.memoravel = memoravel;
        this.latitude = latitude;
        this.longitude = longitude;
        this.temperatura = temperatura;
        this.vento = vento;
        this.condicao = condicao;
    }

    @Exclude
    public String getId() {
        return id;
    }

    @Exclude
    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public boolean isFavorito() {
        return favorito;
    }

    public void setFavorito(boolean favorito) {
        this.favorito = favorito;
    }

    public boolean isMemoravel() {
        return memoravel;
    }

    public void setMemoravel(boolean memoravel) {
        this.memoravel = memoravel;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(String temperatura) {
        this.temperatura = temperatura;
    }

    public String getVento() {
        return vento;
    }

    public void setVento(String vento) {
        this.vento = vento;
    }

    public String getCondicao() {
        return condicao;
    }

    public void setCondicao(String condicao) {
        this.condicao = condicao;
    }
}