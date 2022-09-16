package ucm.tfg.pccomponentes.list;

import ucm.tfg.pccomponentes.R;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
implements View.OnClickListener{

    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;
    private ArrayList<Item> listDatos;
    private View.OnClickListener listener;
    private View view;

    public Adapter(ArrayList<Item> listDatos) {
        this.listDatos = listDatos;
    }

    @NotNull
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        RecyclerView.ViewHolder v=null;
        if(i == VIEW_TYPE_ITEM){
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.component_card,null,false);
            view.setOnClickListener(this);
            v = new ViewHolderDatos(view);
      }
      else {
          view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_loading,null,false);
          v = new ViewHolderCarga(view);
        }
        return v;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if(holder instanceof ViewHolderDatos){
                adaptarItem((ViewHolderDatos) holder,position);
            }
            else if(holder instanceof  ViewHolderCarga){
                showLoadingView((ViewHolderCarga) holder, position);
            }
    }

    private void showLoadingView(ViewHolderCarga holder, int position) {
        Log.d("Carga", "ey");

    }

    private void adaptarItem(ViewHolderDatos vhd, int pos) {
        vhd.categoria.setText(listDatos.get(pos).getCategoria());
        vhd.nombre.setText(listDatos.get(pos).getNombre());
        Picasso.get()
                .load(listDatos.get(pos).getImagen())
                .resize(300,300)
                .into(vhd.foto);
        vhd.precio.setText(String.valueOf(listDatos.get(pos).getPrecio())+ " \u20AC");
    }


    @Override
    public int getItemCount() {
        return listDatos == null ? 0 : listDatos.size();
    }
    public void setOnClickListener(View.OnClickListener listen){
        this.listener = listen;
    }
    @Override
    public void onClick(View v) {
        if(listener != null){
           listener.onClick(v);
        }
    }
    public int getItemViewType(int position) {
        return listDatos.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    private class ViewHolderDatos extends RecyclerView.ViewHolder {
        private TextView nombre,categoria,precio;
        private ImageView foto;
        ViewHolderDatos(@NonNull View itemView) {
            super(itemView);

            nombre = itemView.findViewById(R.id.componentName);
            categoria = itemView.findViewById(R.id.componentCategory);
            foto = itemView.findViewById(R.id.componentImage);
            precio = itemView.findViewById(R.id.componentPrice);
        }

    }
    private class ViewHolderCarga extends RecyclerView.ViewHolder{
        private ProgressBar barra;
        public ViewHolderCarga(@NonNull View itemView) {
            super(itemView);
            barra = itemView.findViewById(R.id.progressBar);
        }
    }
}
