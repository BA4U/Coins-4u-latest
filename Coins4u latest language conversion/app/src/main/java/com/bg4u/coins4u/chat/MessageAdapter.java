package com.bg4u.coins4u.chat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bg4u.coins4u.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    
    private final List<Messages> UserMessageList;
    private DatabaseReference userRef;
    private FirebaseAuth mAuth;
    public MessageAdapter(List<Messages> UserMessageList)
    {
        this.UserMessageList=UserMessageList;
    }
    
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_messages_layout,parent,false);
        MessageViewHolder messageViewHolder=new MessageViewHolder(view);
        mAuth=FirebaseAuth.getInstance();
        
        return messageViewHolder;
    }
    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder holder, final int position) {
        String messagesenderid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        final Messages messages = UserMessageList.get(holder.getAdapterPosition());
        String fromuserid = messages.getFrom();
        String frommessagetype = messages.getType();
        String token = messages.getToken();
        
        userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(fromuserid);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("image")) {
                    String receiverprofileimage = Objects.requireNonNull(dataSnapshot.child("image").getValue()).toString();
                    Picasso.get().load(receiverprofileimage).placeholder(R.drawable.user_icon_default).into(holder.receiverprofileimage);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
        
        holder.receivermessagetext.setVisibility(View.GONE);
        holder.receiverprofileimage.setVisibility(View.GONE);
        holder.sendermessagetext.setVisibility(View.GONE);
        holder.messageSenderPicture.setVisibility(View.GONE);
        holder.messageReceiverPicture.setVisibility(View.GONE);
        if(frommessagetype.equals("text"))
        {
            if(fromuserid.equals(messagesenderid))
            {
                holder.sendermessagetext.setVisibility(View.VISIBLE);
                holder.sendermessagetext.setBackgroundResource(R.drawable.send_message_background);
                holder.sendermessagetext.setText(messages.getMessage()+"\n \n"+messages.getTime()+" - "+messages.getDate());
            }
            else
            {
                holder.receivermessagetext.setVisibility(View.VISIBLE);
                holder.receiverprofileimage.setVisibility(View.VISIBLE);
                holder.receivermessagetext.setBackgroundResource(R.drawable.received_message_background);
                holder.receivermessagetext.setText(messages.getMessage()+"\n \n"+messages.getTime()+" - "+messages.getDate());
            }
        }
        else  if(frommessagetype.equals("image"))
        {
            if(fromuserid.equals(messagesenderid))
            {
                holder.messageSenderPicture.setVisibility(View.VISIBLE);
                Picasso.get().load(messages.getMessage()).into(holder.messageSenderPicture);
            }
            else
            {
                holder.messageReceiverPicture.setVisibility(View.VISIBLE);
                holder.receiverprofileimage.setVisibility(View.VISIBLE);
                Picasso.get().load(messages.getMessage()).into(holder.messageReceiverPicture);
            }
        }
        else if(frommessagetype.equals("pdf") || frommessagetype.equals("all") || frommessagetype.equals("video")) {
            if(fromuserid.equals(messagesenderid)) {
                holder.messageSenderPicture.setVisibility(View.VISIBLE);
                Picasso.get().load("https://firebasestorage.googleapis.com/v0/b/chatapp-9849c.appspot.com/o/Image%20Files%2Ffile.png?alt=media&token=34fb4f27-54c8-4c06-b6de-59b6b8deddd2")
                        .into(holder.messageSenderPicture);
                
            } else {
                holder.messageReceiverPicture.setVisibility(View.VISIBLE);
                Picasso.get().load("https://firebasestorage.googleapis.com/v0/b/chatapp-9849c.appspot.com/o/Image%20Files%2Ffile.png?alt=media&token=34fb4f27-54c8-4c06-b6de-59b6b8deddd2")
                        .into(holder.messageReceiverPicture);
                
                holder.receiverprofileimage.setVisibility(View.VISIBLE);
                
            }
        }
        
        if(fromuserid.equals(messagesenderid))
        {
            holder.itemView.setOnClickListener(v -> {
                if(UserMessageList.get(position).getType().equals("pdf") || UserMessageList.get(position).getType().equals("docx"))
                {
                    CharSequence[] options =new CharSequence[]
                            {
                                    "Delete for me","Download and view content","Cancel","Delete for everyone"
                            };
                    
                    AlertDialog.Builder builder=new AlertDialog.Builder(holder.itemView.getContext());
                    builder.setTitle("Delete Message?");
                    builder.setItems(options, (dialog, which) -> {
                        if(which==0)
                        {
                            deleteSentMessage(position,holder);
                            
                        }else if(which==1)
                        {
                            Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(UserMessageList.get(position).getMessage()));
                            holder.itemView.getContext().startActivity(intent);
                        } else if (which == 2) {
                            //for cancel do not do anything
                        } else if (which == 3) {
                            deleteMessageForEveryone(position, holder);
                        }
                    });
                    
                    builder.show();
                } else if (UserMessageList.get(position).getType().equals("text")) {
                    CharSequence[] options = new CharSequence[]{
                            "Delete for me", "Cancel", "Delete for everyone"
                    };
                    
                    AlertDialog.Builder builder=new AlertDialog.Builder(holder.itemView.getContext());
                    builder.setTitle("Delete Message?");
                    builder.setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            deleteSentMessage(position, holder);
                        } else if (which == 1) {
                            //for cancel do not do anything
                        } else if (which == 2) {
                            deleteMessageForEveryone(position, holder);
                        }
                    });
                    
                    builder.show();
                } else if (UserMessageList.get(position).getType().equals("image")) {
                    CharSequence options[] = new CharSequence[]{
                            "Delete for me", "View This Image", "Cancel", "Delete for everyone"
                    };
                    
                    AlertDialog.Builder builder=new AlertDialog.Builder(holder.itemView.getContext());
                    builder.setTitle("Delete Message?");
                    builder.setItems(options, (dialog, which) -> {
                        if(which==0)
                        {
                            deleteSentMessage(position,holder);
                            
                        }else if(which==1)
                        {
                            Intent intent=new Intent(holder.itemView.getContext(), ImageViewActivity.class);
                            intent.putExtra("url",UserMessageList.get(position).getMessage());
                            holder.itemView.getContext().startActivity(intent);
                            
                        }else if(which==2)
                        {
                            //for cancel do not do anything
                        }
                        else if(which==3)
                        {
                            deleteMessageForEveryone(position,holder);
                            
                        }
                    });
                    
                    builder.show();
                }
            });
        }
        else
        {
            holder.itemView.setOnClickListener(v -> {
                switch (UserMessageList.get(position).getType()) {
                    case "pdf":
                    case "docx": {
                        CharSequence options[] = new CharSequence[]
                                {
                                        "Delete for me", "Download and view content", "Cancel"
                                };

                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete Message?");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    deleteReceiveMessage(position, holder);

                                } else if (which == 1) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(UserMessageList.get(position).getMessage()));
                                    holder.itemView.getContext().startActivity(intent);
                                } else if (which == 2) {
                                    //for cancel do not do anything
                                }

                            }
                        });

                        builder.show();
                        break;
                    }
                    case "text": {
                        CharSequence[] options = new CharSequence[]
                                {
                                        "Delete for me", "Cancel"
                                };
            
                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete Message?");
                        builder.setItems(options, (dialog, which) -> {
                            if (which == 0) {
                                deleteReceiveMessage(position, holder);
                            } else if (which == 1) {
                                //for cancel do not do anything
                            }
                        });
            
                        builder.show();
                        break;
                    }
                    case "image": {
                        CharSequence[] options = new CharSequence[]{
                                "Delete for me", "View This Image", "Cancel"
                        };
            
                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete Message?");
                        builder.setItems(options, (dialog, which) -> {
                            if (which == 0) {
                                deleteReceiveMessage(position, holder);
                    
                            } else if (which == 1) {
                                Intent intent = new Intent(holder.itemView.getContext(), ImageViewActivity.class);
                                intent.putExtra("url", UserMessageList.get(position).getMessage());
                                holder.itemView.getContext().startActivity(intent);
                    
                            } else if (which == 2) {
                                //for cancel do not do anything
                            }
                
                        });
            
                        builder.show();
                        break;
                    }
                }
            });
        }
    }
    private void deleteSentMessage(final int position,final MessageViewHolder holder)
    {
        DatabaseReference rootRef=FirebaseDatabase.getInstance().getReference();
        rootRef.child("Messages").child(UserMessageList.get(position).getFrom())
                .child(UserMessageList.get(position).getTo()).child(UserMessageList.get(position).getMessageID())
                .removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        notifyItemRemoved(position);
                        UserMessageList.remove(position);
                        notifyItemRangeChanged(position, UserMessageList.size());
                        Toast.makeText(holder.itemView.getContext(),"Message deleted...",Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(holder.itemView.getContext(),"Error...",Toast.LENGTH_SHORT).show();
                    }
                });
        
    }
    
    private void deleteReceiveMessage(final int position,final MessageViewHolder holder)
    {
        DatabaseReference rootRef=FirebaseDatabase.getInstance().getReference();
        rootRef.child("Messages").child(UserMessageList.get(position).getTo())
                .child(UserMessageList.get(position).getFrom()).child(UserMessageList.get(position).getMessageID())
                .removeValue().addOnCompleteListener(task -> {
                    if(task.isSuccessful())
                    {
                        notifyItemRemoved(position);
                        UserMessageList.remove(position);
                        notifyItemRangeChanged(position, UserMessageList.size());
                        Toast.makeText(holder.itemView.getContext(),"Message deleted...",Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(holder.itemView.getContext(),"Error...",Toast.LENGTH_SHORT).show();
                    }
                });
        
    }
    
    private void deleteMessageForEveryone(final int position,final MessageViewHolder holder)
    {
        DatabaseReference rootRef=FirebaseDatabase.getInstance().getReference();
        rootRef.child("Messages").child(UserMessageList.get(position).getFrom())
                .child(UserMessageList.get(position).getTo()).child(UserMessageList.get(position).getMessageID())
                .removeValue().addOnCompleteListener(task -> {
                    if(task.isSuccessful())
                    {
                        DatabaseReference rootRef1 =FirebaseDatabase.getInstance().getReference();
                        rootRef1.child("Messages").child(UserMessageList.get(position).getTo())
                                .child(UserMessageList.get(position).getFrom()).child(UserMessageList.get(position).getMessageID())
                                .removeValue().addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        notifyItemRemoved(position);
                                        UserMessageList.remove(position);
                                        notifyItemRangeChanged(position, UserMessageList.size());
                                        Toast.makeText(holder.itemView.getContext(),"Message deleted...",Toast.LENGTH_SHORT).show();
                                    }
                                    else
                                    {
                                        Toast.makeText(holder.itemView.getContext(),"Error...",Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(holder.itemView.getContext(), "Error...", Toast.LENGTH_SHORT).show();
                    }
                });
        
    }
    @Override
    public int getItemCount() {
        return UserMessageList.size();
    }
    
    public class MessageViewHolder extends RecyclerView.ViewHolder {
        
        public TextView sendermessagetext,receivermessagetext;
        public CircleImageView
                receiverprofileimage;
        public ImageView messageSenderPicture,messageReceiverPicture;
        
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            sendermessagetext=itemView.findViewById(R.id.sender_message_text);
            receivermessagetext=itemView.findViewById(R.id.receiver_message_text);
            receiverprofileimage=itemView.findViewById(R.id.message_profile_image);
            messageSenderPicture=itemView.findViewById(R.id.message_sender_image_view);
            messageReceiverPicture=itemView.findViewById(R.id.message_receiver_image_view);
        }
    }
    
    
}