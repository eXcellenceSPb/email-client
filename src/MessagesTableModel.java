import java.util.*;
import javax.mail.*;
import javax.swing.table.*;

public class MessagesTableModel extends AbstractTableModel {
    private static final String[] columnNames = {"Sender", "Subject", "Date"};

    private ArrayList<Message> messageList = new ArrayList<>();

    public void setMessages(Message[] messages) {
        for (int i = messages.length - 1; i >= 0; i--) {
            messageList.add(messages[i]);
        }
        fireTableDataChanged();
    }

    public Message getMessage(int row) {
        return messageList.get(row);
    }

    public void deleteMessage(int row) {
        messageList.remove(row);
        fireTableRowsDeleted(row, row);
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public int getRowCount() {
        return messageList.size();
    }

    public Object getValueAt(int row, int col) {
        try {
            Message message = messageList.get(row);
            switch (col) {
                case 0: // Sender
                    Address[] senders = message.getFrom();
                    if (senders != null || senders.length > 0) {
                        return senders[0].toString();
                    } else {
                        return "[none]";
                    }
                case 1: // Subject
                    String subject = message.getSubject();
                    if (subject != null && subject.length() > 0) {
                        return subject;
                    } else {
                        return "[none]";
                    }
                case 2: // Date
                    Date date = message.getSentDate();
                    if (date != null) {
                        return date.toString();
                    } else {
                        return "[none]";
                    }
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }
}