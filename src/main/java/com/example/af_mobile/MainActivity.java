package com.example.af_mobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int CODIGO_PERMISSAO_LOCALIZACAO = 100;

    EditText edtTitulo, edtDescricao, edtData;
    Spinner spnCategoria;
    Switch swtFavorito, swtMemoravel;
    TextView txtClima;
    Button btnBuscarClima, btnSalvar, btnLimpar;
    ListView lstVisitas;

    FirebaseFirestore db;
    FusedLocationProviderClient fusedLocationClient;

    ArrayList<Visita> listaVisitas = new ArrayList<>();
    ArrayList<String> listaTextos = new ArrayList<>();
    ArrayAdapter<String> adapterVisitas;

    Visita visitaEditando = null;

    double latitudeAtual = 0;
    double longitudeAtual = 0;
    String temperaturaAtual = "";
    String ventoAtual = "";
    String condicaoAtual = "";
    boolean temClima = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        edtTitulo = findViewById(R.id.txtTitulo);
        edtDescricao = findViewById(R.id.txtDescricao);
        edtData = findViewById(R.id.txtData);
        spnCategoria = findViewById(R.id.spnCategoria);
        swtFavorito = findViewById(R.id.swtFavorito);
        swtMemoravel = findViewById(R.id.swtMemoravel);
        txtClima = findViewById(R.id.txtClima);
        btnBuscarClima = findViewById(R.id.btnBuscarClima);
        btnSalvar = findViewById(R.id.btnSalvar);
        btnLimpar = findViewById(R.id.btnLimpar);
        lstVisitas = findViewById(R.id.lstVisitas);

        configurarSpinner();

        adapterVisitas = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaTextos);
        lstVisitas.setAdapter(adapterVisitas);

        btnBuscarClima.setOnClickListener(v -> verificarPermissaoLocalizacao());
        btnSalvar.setOnClickListener(v -> salvarVisita());
        btnLimpar.setOnClickListener(v -> limparCampos());

        lstVisitas.setOnItemClickListener((parent, view, position, id) -> {
            Visita visita = listaVisitas.get(position);
            carregarParaEdicao(visita);
            mostrarDetalhes(visita);
        });

        lstVisitas.setOnItemLongClickListener((parent, view, position, id) -> {
            Visita visita = listaVisitas.get(position);
            confirmarExclusao(visita);
            return true;
        });

        carregarVisitas();
    }

    private void configurarSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.opcoesCategoria,
                android.R.layout.simple_spinner_item
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCategoria.setAdapter(adapter);
    }

    private void verificarPermissaoLocalizacao() {
        boolean permissaoFina = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        boolean permissaoAproximada = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        if (permissaoFina || permissaoAproximada) {
            capturarLocalizacao();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    CODIGO_PERMISSAO_LOCALIZACAO
            );
        }
    }

    @SuppressLint("MissingPermission")
    private void capturarLocalizacao() {
        Toast.makeText(this, "Capturando localização...", Toast.LENGTH_SHORT).show();

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        latitudeAtual = location.getLatitude();
                        longitudeAtual = location.getLongitude();

                        buscarClimaNaApi(latitudeAtual, longitudeAtual);
                    } else {
                        Toast.makeText(this, "Não foi possível obter a localização.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao capturar localização: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void buscarClimaNaApi(double latitude, double longitude) {
        txtClima.setText("Buscando clima na API...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String endereco = String.format(
                            Locale.US,
                            "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current=temperature_2m,wind_speed_10m,weather_code&timezone=auto",
                            latitude,
                            longitude
                    );

                    URL url = new URL(endereco);
                    HttpURLConnection conexao = (HttpURLConnection) url.openConnection();
                    conexao.setRequestMethod("GET");
                    conexao.setConnectTimeout(10000);
                    conexao.setReadTimeout(10000);

                    int responseCode = conexao.getResponseCode();

                    if (responseCode == 200) {
                        BufferedReader resposta = new BufferedReader(
                                new InputStreamReader(conexao.getInputStream())
                        );

                        String linha;
                        String jsonEmString = "";

                        while ((linha = resposta.readLine()) != null) {
                            jsonEmString += linha;
                        }

                        resposta.close();
                        conexao.disconnect();

                        Gson gson = new Gson();
                        Type tipoClima = new TypeToken<Clima>() {}.getType();
                        Clima clima = gson.fromJson(jsonEmString, tipoClima);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (clima != null && clima.getCurrent() != null) {
                                    double temperatura = clima.getCurrent().getTemperature2m();
                                    double vento = clima.getCurrent().getWindSpeed10m();
                                    int codigoClima = clima.getCurrent().getWeatherCode();

                                    temperaturaAtual = String.format(Locale.getDefault(), "%.1f", temperatura);
                                    ventoAtual = String.format(Locale.getDefault(), "%.1f", vento);
                                    condicaoAtual = traduzirCodigoClima(codigoClima);
                                    temClima = true;

                                    txtClima.setText(montarTextoClima());

                                    Toast.makeText(MainActivity.this, "GPS e clima carregados!", Toast.LENGTH_SHORT).show();
                                } else {
                                    txtClima.setText("Erro ao ler dados do clima.");
                                    Toast.makeText(MainActivity.this, "Erro ao converter JSON da API.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                txtClima.setText("Erro na API. Código: " + responseCode);
                                Toast.makeText(MainActivity.this, "Erro na API. Código: " + responseCode, Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                } catch (Exception e) {
                    String mensagem = e.getMessage();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtClima.setText("Erro ao buscar clima.");
                            Toast.makeText(MainActivity.this, "Erro: " + mensagem, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void salvarVisita() {
        if (!validarCampos()) {
            return;
        }

        String titulo = edtTitulo.getText().toString().trim();
        String descricao = edtDescricao.getText().toString().trim();
        String data = edtData.getText().toString().trim();
        String categoria = spnCategoria.getSelectedItem().toString();
        boolean favorito = swtFavorito.isChecked();
        boolean memoravel = swtMemoravel.isChecked();

        if (visitaEditando == null) {
            Visita visita = new Visita(
                    titulo,
                    descricao,
                    data,
                    categoria,
                    favorito,
                    memoravel,
                    latitudeAtual,
                    longitudeAtual,
                    temperaturaAtual,
                    ventoAtual,
                    condicaoAtual
            );

            db.collection("visitas")
                    .add(visita)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Visita salva!", Toast.LENGTH_SHORT).show();
                        limparCampos();
                        carregarVisitas();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Erro ao salvar visita: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        } else {
            visitaEditando.setTitulo(titulo);
            visitaEditando.setDescricao(descricao);
            visitaEditando.setData(data);
            visitaEditando.setCategoria(categoria);
            visitaEditando.setFavorito(favorito);
            visitaEditando.setMemoravel(memoravel);
            visitaEditando.setLatitude(latitudeAtual);
            visitaEditando.setLongitude(longitudeAtual);
            visitaEditando.setTemperatura(temperaturaAtual);
            visitaEditando.setVento(ventoAtual);
            visitaEditando.setCondicao(condicaoAtual);

            db.collection("visitas")
                    .document(visitaEditando.getId())
                    .set(visitaEditando)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Visita atualizada!", Toast.LENGTH_SHORT).show();
                        limparCampos();
                        carregarVisitas();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Erro ao atualizar visita: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private boolean validarCampos() {
        String titulo = edtTitulo.getText().toString().trim();
        String descricao = edtDescricao.getText().toString().trim();
        String data = edtData.getText().toString().trim();

        if (titulo.isEmpty()) {
            edtTitulo.setError("Informe o título.");
            edtTitulo.requestFocus();
            return false;
        }

        if (descricao.isEmpty()) {
            edtDescricao.setError("Informe a descrição.");
            edtDescricao.requestFocus();
            return false;
        }

        if (data.isEmpty()) {
            edtData.setError("Informe a data.");
            edtData.requestFocus();
            return false;
        }

        if (spnCategoria.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Selecione uma categoria.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!temClima) {
            Toast.makeText(this, "Busque o GPS e o clima antes de salvar.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void carregarVisitas() {
        db.collection("visitas")
                .get()
                .addOnSuccessListener(query -> {
                    listaVisitas.clear();
                    listaTextos.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        Visita visita = doc.toObject(Visita.class);
                        visita.setId(doc.getId());

                        listaVisitas.add(visita);

                        String texto = visita.getTitulo()
                                + "\nData: " + visita.getData()
                                + " | Categoria: " + visita.getCategoria()
                                + "\nTemp: " + visita.getTemperatura() + "°C"
                                + " | Clima: " + visita.getCondicao()
                                + "\nLocal: " + String.format(Locale.getDefault(), "%.5f", visita.getLatitude())
                                + ", " + String.format(Locale.getDefault(), "%.5f", visita.getLongitude())
                                + "\nFavorita: " + (visita.isFavorito() ? "Sim" : "Não")
                                + " | Memorável: " + (visita.isMemoravel() ? "Sim" : "Não");

                        listaTextos.add(texto);
                    }

                    adapterVisitas.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao carregar visitas.", Toast.LENGTH_SHORT).show();
                });
    }

    private void carregarParaEdicao(Visita visita) {
        visitaEditando = visita;

        edtTitulo.setText(visita.getTitulo());
        edtDescricao.setText(visita.getDescricao());
        edtData.setText(visita.getData());
        selecionarCategoria(visita.getCategoria());
        swtFavorito.setChecked(visita.isFavorito());
        swtMemoravel.setChecked(visita.isMemoravel());

        latitudeAtual = visita.getLatitude();
        longitudeAtual = visita.getLongitude();
        temperaturaAtual = visita.getTemperatura();
        ventoAtual = visita.getVento();
        condicaoAtual = visita.getCondicao();
        temClima = true;

        txtClima.setText(montarTextoClima());
        btnSalvar.setText("Atualizar visita");

        Toast.makeText(this, "Visita carregada para edição.", Toast.LENGTH_SHORT).show();
    }

    private void confirmarExclusao(Visita visita) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir visita")
                .setMessage("Deseja excluir \"" + visita.getTitulo() + "\"?")
                .setPositiveButton("Excluir", (dialog, which) -> {
                    db.collection("visitas")
                            .document(visita.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Visita excluída!", Toast.LENGTH_SHORT).show();
                                limparCampos();
                                carregarVisitas();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Erro ao excluir visita.", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDetalhes(Visita visita) {
        String detalhes = "Descrição: " + visita.getDescricao()
                + "\n\nData: " + visita.getData()
                + "\nCategoria: " + visita.getCategoria()
                + "\nFavorita: " + (visita.isFavorito() ? "Sim" : "Não")
                + "\nMemorável: " + (visita.isMemoravel() ? "Sim" : "Não")
                + "\n\nLatitude: " + visita.getLatitude()
                + "\nLongitude: " + visita.getLongitude()
                + "\n\nTemperatura: " + visita.getTemperatura() + "°C"
                + "\nVento: " + visita.getVento() + " km/h"
                + "\nCondição: " + visita.getCondicao();

        new AlertDialog.Builder(this)
                .setTitle(visita.getTitulo())
                .setMessage(detalhes)
                .setPositiveButton("OK", null)
                .show();
    }

    private String montarTextoClima() {
        return "Latitude: " + String.format(Locale.getDefault(), "%.5f", latitudeAtual)
                + "\nLongitude: " + String.format(Locale.getDefault(), "%.5f", longitudeAtual)
                + "\nTemperatura: " + temperaturaAtual + "°C"
                + "\nVento: " + ventoAtual + " km/h"
                + "\nCondição: " + condicaoAtual;
    }

    private void selecionarCategoria(String categoria) {
        if (categoria == null) {
            return;
        }

        for (int i = 0; i < spnCategoria.getCount(); i++) {
            String item = spnCategoria.getItemAtPosition(i).toString();

            if (item.equalsIgnoreCase(categoria)) {
                spnCategoria.setSelection(i);
                return;
            }
        }
    }

    private void limparCampos() {
        edtTitulo.setText("");
        edtDescricao.setText("");
        edtData.setText("");
        spnCategoria.setSelection(0);
        swtFavorito.setChecked(false);
        swtMemoravel.setChecked(false);

        latitudeAtual = 0;
        longitudeAtual = 0;
        temperaturaAtual = "";
        ventoAtual = "";
        condicaoAtual = "";
        temClima = false;

        visitaEditando = null;
        btnSalvar.setText("Salvar visita");
        txtClima.setText("Clima ainda não buscado.");
    }

    private String traduzirCodigoClima(int codigo) {
        switch (codigo) {
            case 0:
                return "Céu limpo";
            case 1:
            case 2:
            case 3:
                return "Parcialmente nublado";
            case 45:
            case 48:
                return "Neblina";
            case 51:
            case 53:
            case 55:
                return "Garoa";
            case 61:
            case 63:
            case 65:
                return "Chuva";
            case 71:
            case 73:
            case 75:
                return "Neve";
            case 80:
            case 81:
            case 82:
                return "Pancadas de chuva";
            case 95:
                return "Tempestade";
            case 96:
            case 99:
                return "Tempestade com granizo";
            default:
                return "Código do clima: " + codigo;
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CODIGO_PERMISSAO_LOCALIZACAO) {
            boolean permitido = false;

            for (int resultado : grantResults) {
                if (resultado == PackageManager.PERMISSION_GRANTED) {
                    permitido = true;
                    break;
                }
            }

            if (permitido) {
                capturarLocalizacao();
            } else {
                Toast.makeText(this, "Permissão de localização negada.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}