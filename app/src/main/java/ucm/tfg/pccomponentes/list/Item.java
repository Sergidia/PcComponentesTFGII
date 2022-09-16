package ucm.tfg.pccomponentes.list;

import java.io.Serializable;

public class Item implements Serializable {

    private String codigo;
    private String nombre;
    private String imagen;
    private String url;
    private String categoria;
    private boolean valida;
    private double precio;

    public Item(String cod, String name, String img, double price, String link, String categ, boolean valid) {
        this.codigo = cod;
        this.nombre = name;
        this.imagen = img;
        this.precio = price;
        this.categoria = categ;
        this.url = link;
        this.valida = valid;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public boolean isValida() {
        return valida;
    }

    public void setValida(boolean valida) {
        this.valida = valida;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String img) {
        this.imagen = img;
    }
}
