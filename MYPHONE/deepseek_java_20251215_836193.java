package com.example.phonebook;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {
    
    private ArrayList<Contact> contacts;
    private OnItemClickListener listener;
    private boolean editMode = false;
    
    public interface OnItemClickListener {
        void onItemClick(Contact contact);
    }
    
    public ContactAdapter(ArrayList<Contact> contacts, OnItemClickListener listener) {
        this.contacts = contacts;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.contact_item, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contact contact = contacts.get(position);
        holder.tvName.setText(contact.getName());
        holder.tvNumber.setText(formatPhoneNumber(contact.getNumber()));
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(contact);
            }
        });
        
        // في وضع التعديل، جعل النصوص قابلة للتعديل
        holder.tvName.setEnabled(editMode);
        holder.tvNumber.setEnabled(editMode);
    }
    
    @Override
    public int getItemCount() {
        return contacts.size();
    }
    
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
        notifyDataSetChanged();
    }
    
    public boolean isEditMode() {
        return editMode;
    }
    
    private String formatPhoneNumber(String number) {
        if (number == null) return "";
        // تنسيق الرقم للعرض
        return number.replaceAll("(\\d{3})(\\d{3})(\\d{4})", "$1-$2-$3");
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvNumber, tvExtraNumbers;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvNumber = itemView.findViewById(R.id.tv_number);
            tvExtraNumbers = itemView.findViewById(R.id.tv_extra_numbers);
        }
    }
}