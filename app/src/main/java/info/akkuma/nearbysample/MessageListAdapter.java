package info.akkuma.nearbysample;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MessageListAdapter extends ArrayAdapter<ChatMessage> {

    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("HH:mm");

    public MessageListAdapter(Context context, List<ChatMessage> objects) {
        super(context, R.layout.list_item_message, R.id.message_text, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        ChatMessage message = getItem(position);


        TextView nameText = (TextView) view.findViewById(R.id.name_text);
        nameText.setText(message.getName());

        TextView messageText = (TextView) view.findViewById(R.id.message_text);
        messageText.setText(message.getText());

        TextView timestampText = (TextView) view.findViewById(R.id.timestamp_text);
        timestampText.setText(mSimpleDateFormat.format(new Date(message.getTimestamp())));

        return view;
    }
}
