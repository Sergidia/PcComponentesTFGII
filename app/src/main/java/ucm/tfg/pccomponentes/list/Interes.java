package ucm.tfg.pccomponentes.list;

import java.io.Serializable;

public class Interes implements Serializable {

    private  String codigo;
    private Double precioMax;

    public Interes(String codigo, Double precioMax) {
        this.codigo = codigo;
        this.precioMax = precioMax;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Double getPrecioMax() {
        return precioMax;
    }

    public void setPrecioMax(Double precioMax) {
        this.precioMax = precioMax;
    }

}
