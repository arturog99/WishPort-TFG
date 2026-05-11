package com.wishport.frontend.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wishport.frontend.R;
import com.wishport.frontend.models.Reserva;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ADAPTADOR DE RESERVAS: Transforma la lista de reservas del usuario en elementos visuales.
 * Se usa en la pantalla "Mis Reservas" (ReservasActivity) y en AdminActivity.
 *
 * Funciona igual que PistaAdapter: toma una lista de objetos Reserva
 * y los convierte en filas visuales dentro de un RecyclerView.
 */
public class ReservaAdapter extends RecyclerView.Adapter<ReservaAdapter.ReservaViewHolder> {

    /** Lista de reservas que se muestran actualmente */
    private List<Reserva> listaReservas;
    /** Formatea la fecha como dd/MM, ej: "15/05" */
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM");
    /** Formatea la hora como HH:mm, ej: "18:00" */
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    /** Callback que se ejecuta cuando el usuario pulsa una reserva */
    private OnItemClickListener listener;

    /**
     * Interfaz de callback para que ReservasActivity reciba el clic
     * en una reserva concreta y pueda abrir DetalleReservaActivity.
     */
    public interface OnItemClickListener {
        /** Se ejecuta cuando el usuario pulsa el item de una reserva */
        void onItemClick(Reserva reserva);
    }

    /**
     * Registra el listener que recibirá los clics en las reservas.
     * @param listener Objeto que implementa OnItemClickListener.
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Constructor del adaptador.
     * @param listaReservas Lista de reservas a mostrar en el RecyclerView.
     */
    public ReservaAdapter(List<Reserva> listaReservas) {
        this.listaReservas = listaReservas;
    }

    /**
     * Reemplaza la lista actual por una nueva y refresca toda la pantalla.
     * Se usa en AdminActivity para actualizar reservas después de filtrarlas por fecha.
     * notifyDataSetChanged() obliga al RecyclerView a redibujar todos los items.
     * @param nuevaLista Nueva lista de reservas a mostrar.
     */
    public void actualizarLista(List<Reserva> nuevaLista) {
        this.listaReservas = nuevaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReservaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reserva, parent, false);
        return new ReservaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservaViewHolder holder, int position) {
        Reserva reserva = listaReservas.get(position);

        // 1. Mostrar Deporte
        String deporte = (reserva.getIdPista() != null) ? reserva.getIdPista().getDeporte() : "Deporte";
        holder.tvDeporte.setText(deporte);

        // 2. Formatear Fecha y Hora (Ej: Día 15/05 - 18:00h)
        String fecha = (reserva.getFecha() != null) ? reserva.getFecha().format(dateFormatter) : "--/--";
        String hora = (reserva.getHoraInicio() != null) ? reserva.getHoraInicio().format(timeFormatter) : "--:--";
        holder.tvInfoReserva.setText("Día " + fecha + " - " + hora + "h");

        // 3. Click para ver el QR
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(reserva);
        });
    }

    @Override
    public int getItemCount() {
        return listaReservas != null ? listaReservas.size() : 0;
    }

    static class ReservaViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeporte, tvInfoReserva;

        public ReservaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeporte = itemView.findViewById(R.id.tvDeporte);
            tvInfoReserva = itemView.findViewById(R.id.tvInfoReserva);
        }
    }
}
