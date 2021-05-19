package com.example.androidftpclient.Adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidftpclient.R;

import org.apache.commons.net.ftp.FTPFile;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(FTPFile file);
    }
    private OnItemClickListener onItemClickListener;
    public void setOnItemClickListener(OnItemClickListener onItemClickListener){
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(FTPFile file);
    }
    private OnDeleteClickListener onDeleteClickListener;
    public void setOnDeleteClickListener(OnDeleteClickListener onDeleteClickListener){
        this.onDeleteClickListener = onDeleteClickListener;
    }

    private FTPFile[] fileArray;
    private final LayoutInflater inflater;
    private static SimpleDateFormat format =
            new SimpleDateFormat("yyyy年MM月dd日 | HH:mm", Locale.getDefault());
    public static final int DIRECTORY_OPERATION = 1;
    private int type = 0;

    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextView fileNameView;
        TextView fileInfoView;
        TextView btnDelete;

        public ViewHolder(View view){
            super(view);
            fileNameView = view.findViewById(R.id.view_file_name);
            fileInfoView = view.findViewById(R.id.view_file_info);
            btnDelete = view.findViewById(R.id.btn_delete);
        }
    }
    public FileAdapter (Context context) {
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.file_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClickListener.onItemClick(fileArray[viewHolder.getAdapterPosition()]);
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(FileAdapter.ViewHolder holder, int position) {
        holder.fileNameView.setText(fileArray[position].getName());
        holder.fileInfoView.setText(format.format(fileArray[position].getTimestamp().getTime()));
        if (type != DIRECTORY_OPERATION) {
            holder.btnDelete.setVisibility(View.GONE);
        } else {
            holder.btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onDeleteClickListener.onDeleteClick(fileArray[position]);
                }
            });
        }
        if (fileArray[position].getType() != FTPFile.DIRECTORY_TYPE) {
            holder.fileNameView.setTextColor(Color.BLACK);
        } else {
            holder.fileNameView.setTextColor(Color.parseColor("#FF5722"));
        }
        System.out.println(fileArray[position].getName()+" "+fileArray[position].getType());
    }

    @Override
    public int getItemCount() {
        if (fileArray==null)
            return 0;
        return fileArray.length;
    }

    public void setFiles(FTPFile[] files){
        fileArray = files;
        notifyDataSetChanged();
    }

    public void setDirectoryType(){
        type = DIRECTORY_OPERATION;
    }
}
