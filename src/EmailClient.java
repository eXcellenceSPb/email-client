import javafx.stage.WindowEvent;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Date;
import java.util.Properties;

public class EmailClient extends JFrame {
    private MessagesTableModel tableModel;
    private JTable table;
    private JTextArea messageTextArea;
    private JSplitPane splitPane;
    private JButton replyButton, forwardButton, deleteButton;
    private Message selectedMessage;
    private boolean deleting;
    private Session session;

    public EmailClient() {
        setTitle("E-mail Client");
        setSize(640, 480);

        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem fileExitMenuItem = new JMenuItem("Exit", KeyEvent.VK_X);
        fileExitMenuItem.addActionListener(e -> actionExit());
        fileMenu.add(fileExitMenuItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        JPanel buttonPanel = new JPanel();

        JButton newButton = new JButton("New Message");
        newButton.addActionListener(e -> actionNew());
        buttonPanel.add(newButton);

        tableModel = new MessagesTableModel();
        table = new JTable(tableModel);
        table.getSelectionModel().addListSelectionListener(e -> tableSelectionChanged());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel emailsPanel = new JPanel();
        emailsPanel.setBorder(BorderFactory.createTitledBorder("E-mails"));

        messageTextArea = new JTextArea();
        messageTextArea.setEditable(false);
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(table),
                new JScrollPane(messageTextArea));
        emailsPanel.setLayout(new BorderLayout());
        emailsPanel.add(splitPane, BorderLayout.CENTER);

        JPanel buttonPanel2 = new JPanel();
        replyButton = new JButton("Reply");
        replyButton.addActionListener(e -> actionReply());
        replyButton.setEnabled(false);
        buttonPanel2.add(replyButton);

        forwardButton = new JButton("Forward");
        forwardButton.addActionListener(e -> actionForward());
        forwardButton.setEnabled(false);
        buttonPanel2.add(forwardButton);

        deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> actionDelete());
        deleteButton.setEnabled(false);
        buttonPanel2.add(deleteButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buttonPanel, BorderLayout.NORTH);
        getContentPane().add(emailsPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel2, BorderLayout.SOUTH);
    }


    private void actionExit() {
        System.exit(0);
    }

    private void actionNew() {
        sendMessage(MessageDialog.NEW, null);
    }

    private void tableSelectionChanged() {
        if (!deleting) {
            selectedMessage = tableModel.getMessage(table.getSelectedRow());
            showSelectedMessage();
            updateButtons();
        }
    }

    private void actionReply() {
        sendMessage(MessageDialog.REPLY, selectedMessage);
    }

    private void actionForward() {
        sendMessage(MessageDialog.FORWARD, selectedMessage);
    }

    private void actionDelete() {
        deleting = true;
        try {
            selectedMessage.setFlag(Flags.Flag.DELETED, true);
            Folder folder = selectedMessage.getFolder();
            folder.close(true);
            folder.open(Folder.READ_WRITE);
        } catch (Exception e) {
            showError("Unable to delete message.", false);
        }
        tableModel.deleteMessage(table.getSelectedRow());
        messageTextArea.setText("");
        deleting = false;
        selectedMessage = null;
        updateButtons();
    }

    private void sendMessage(int type, Message message) {
        MessageDialog dialog;
        try {
            dialog = new MessageDialog(this, type, message);
            if (!dialog.display()) {
                return;
            }
        } catch (Exception e) {
            showError("Unable to send message.", false);
            return;
        }

        try {
            Message newMessage = new MimeMessage(session);
            newMessage.setFrom(new InternetAddress(dialog.getFrom()));
            newMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(dialog.getTo()));
            newMessage.setSubject(dialog.getSubject());
            newMessage.setSentDate(new Date());
            if (dialog.getAttachedFile() != null) {
                BodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setText(dialog.getContent());
                Multipart multipart = new MimeMultipart();
                DataSource source = new FileDataSource(dialog.getAttachedFile());
                messageBodyPart.setDataHandler(new DataHandler(source));
                messageBodyPart.setFileName(dialog.getAttachedFile().getName());
                multipart.addBodyPart(messageBodyPart);
                newMessage.setContent(multipart);
            } else {
                newMessage.setText(dialog.getContent());
            }
            Transport.send(newMessage);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Unable to send message.", false);
        }
    }

    private void showSelectedMessage() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            messageTextArea.setText(getMessageContent(selectedMessage));
            messageTextArea.setCaretPosition(0);
        } catch (Exception e) {
            showError("Unable to load message.", false);
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void updateButtons() {
        if (selectedMessage != null) {
            replyButton.setEnabled(true);
            forwardButton.setEnabled(true);
            deleteButton.setEnabled(true);
        } else {
            replyButton.setEnabled(false);
            forwardButton.setEnabled(false);
            deleteButton.setEnabled(false);
        }
    }

    public void show() {
        super.show();
        splitPane.setDividerLocation(.5);
    }

    public void connect() {
        ConnectDialog dialog = new ConnectDialog(this);
        dialog.show();

        final DownloadingDialog downloadingDialog = new DownloadingDialog(this);
        SwingUtilities.invokeLater(downloadingDialog::show);
        Store store = null;
        Properties props = new Properties();
        if (dialog.getTypes().equals("yandex")) {
            try {
                props.put("mail.smtp.host", "true");
                props.put("mail.debug", "false");
                props.put("mail.store.protocol", "imaps");
                props.put("mail.imap.ssl.enable", "true");
                props.put("mail.imap.port", "993");
                props.put("mail.smtp.port", "465");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.socketFactory.port", "465");
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.fallback", "false");
                props.setProperty("mail.smtp.quitwait", "false");
                props.setProperty("mail.transport.protocol", "smtp");

                String login = dialog.getUsername();
                String pass = dialog.getPassword();
                Authenticator auth = new EmailAuthenticator(login, pass);

                session = Session.getDefaultInstance(props, auth);
                session.setDebug(false);
                store = session.getStore();

                String server = "imap.yandex.ru";
                store.connect(server, login, pass);
            } catch (Exception e) {
                e.printStackTrace();
                downloadingDialog.dispose();
                showError("Failed to connect Yandex.\n Check your email or password", true);
            }
        } else {
            try {
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.debug", "false");
                props.put("mail.store.protocol", "imaps");
                props.put("mail.imap.ssl.enable", "true");
                props.put("mail.imap.port", "993");
                props.put("mail.smtp.port", "587");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.socketFactory.port", "587");
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.fallback", "false");
                props.setProperty("mail.smtp.quitwait", "false");
                props.setProperty("mail.transport.protocol", "smtp");

                String login = dialog.getUsername();
                String pass = dialog.getPassword();
                Authenticator auth = new EmailAuthenticator(login, pass);

                session = Session.getDefaultInstance(props, auth);
                session.setDebug(false);
                store = session.getStore();

                String server = "imap.gmail.com";
                store.connect(server, login, pass);
            } catch (Exception e) {
                downloadingDialog.dispose();
                showError("Failed to connect GMail.\n Check your email or password", true);
            }
        }

        try {
            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);
            Message[] messages = folder.getMessages();
            FetchProfile profile = new FetchProfile();
            profile.add(FetchProfile.Item.ENVELOPE);
            folder.fetch(messages, profile);
            tableModel.setMessages(messages);
        } catch (
                Exception e)

        {
            downloadingDialog.dispose();
            showError("Unable to download messages.", true);
        }
        downloadingDialog.dispose();
    }

    private void showError(String message, boolean exit) {
        JOptionPane.showMessageDialog(this,
                message, "Error",
                JOptionPane.ERROR_MESSAGE);
        if (exit)
            System.exit(0);
    }

    public static String getMessageContent(Message message) throws Exception {
        Object content = message.getContent();
        if (content instanceof Multipart) {
            StringBuffer messageContent = new StringBuffer();
            Multipart multipart = (Multipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                Part part = multipart.getBodyPart(i);
                if (part.isMimeType("text/plain")) {
                    messageContent.append(part.getContent().toString());
                }
            }
            return messageContent.toString();
        } else {
            return content.toString();
        }
    }

    public static void main(String[] args) {
        EmailClient client = new EmailClient();
        client.show();
        client.connect();
    }
}