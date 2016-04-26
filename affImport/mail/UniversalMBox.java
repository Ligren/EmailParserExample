package com.example.affImport.mail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.UIDFolder;
import javax.mail.URLName;
import javax.mail.event.StoreEvent;
import javax.mail.event.StoreListener;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;

import com.example.affImport.templates.Template;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import com.borland.dx.dataset.Column;
import com.borland.dx.dataset.ParameterRow;
import com.borland.dx.dataset.Variant;
import com.borland.dx.sql.dataset.Database;
import com.borland.dx.sql.dataset.QueryDataSet;
import com.borland.dx.sql.dataset.QueryDescriptor;
import com.example.affImport.ImportUtils;
import com.example.affImport.app.AffiliateJobsImportFrame;
import com.expl.dbaccess.Connection;
import com.expl.dblib.company.Company;
import com.expl.dblib.company.CompanyParam;
import com.expl.dblib.pool.PoolObject;
import com.expl.dblib.wsObj.v1_05.jobObj.JobObj;
import com.sun.mail.imap.IMAPFolder;

public class UniversalMBox {

    static String protocol;
    static String host = null;
    static String user = null;
    static String password = null;
    static String mbox = null;
    static String url = null;
    static int port = -1;
    static boolean verbose = false;
    static boolean debug = false;
    static boolean showStructure = false;
    static boolean showMessage = false;
    static boolean showAlert = false;
    static boolean saveAttachments = false;
    public static String ADDR_DUBLICATE_JOB;
    private static String contentForParse;
    private static String contentHtml;
    private static long lastUID;
    public static final Database DB = Connection.getNewAddressInstance();
    public static final PoolObject POOL_OBJ = ImportUtils.getPObj();
    public static final CompanyParam COMP_PARAM = Company.getCurCompanyParam();
    private String fileName;

    public void run() throws Exception {

        setConnectionProp();
        System.out.println("We got connection");

        Folder folder = getFolder("INBOX/Reservations");
        IMAPFolder fld = (IMAPFolder) folder;
        System.out.println("Our folder = " + fld.getFullName());

        if (lastUID == 0) {
            if (lastUID < 77920L) lastUID = 77920L;
        }

        System.out.println("Starting to check folder");
        System.out.println("Last UID = " + lastUID);
        Message[] msgs = fld.getMessagesByUID(lastUID, UIDFolder.LASTUID);

        for (int j = 1; j < msgs.length; j++) {

            lastUID = fld.getUID(msgs[j]);

            Message ms = msgs[j];
            System.out.println("I found new email from: " + ms.getFrom()[0] + ", subject: " + ms.getSubject());
            AffiliateJobsImportFrame.updateUI("===================== New Message =====================");
            AffiliateJobsImportFrame.updateUI("I found new email from: " + ms.getFrom()[0] + ", subject: " + ms.getSubject());
            uid = fld.getUID(ms);
            treateEmail(ms);
        }

        folder.close(false);
        store.close();
    }

    Store store;

    private Folder getFolder(String foldersName) throws Exception {
        // Get a Properties object
        Properties props = System.getProperties();
        props.setProperty("mail.mime.address.strict", "false");
        // Get a Session object
        Session session = Session.getInstance(props, null);
        session.setDebug(debug);

        if (showMessage) {
            MimeMessage msg;
            if (mbox != null)
                msg = new MimeMessage(session,
                        new BufferedInputStream(new FileInputStream(mbox)));
            else
                msg = new MimeMessage(session, System.in);
            dumpPart(msg);
            System.exit(0);
        }

        // Get a Store object
        store = null;
        if (url != null) {
            URLName urln = new URLName(url);
            store = session.getStore(urln);
            if (showAlert) {
                store.addStoreListener(new StoreListener() {
                    public void notification(StoreEvent e) {
                        String s;
                        if (e.getMessageType() == StoreEvent.ALERT)
                            s = "ALERT: ";
                        else
                            s = "NOTICE: ";
                        // System.out.println(s + e.getMessage());
                    }
                });
            }
            store.connect();
        } else {
            if (protocol != null)
                store = session.getStore(protocol);
            else
                store = session.getStore();

            // Connect
            if (host != null || user != null || password != null)
                store.connect(host, port, user, password);
            else
                store.connect();
        }

        // Open the Folder

        Folder folder = store.getDefaultFolder();
        if (folder == null) {
            System.out.println("No default folder");
            System.exit(1);
        }
        store.close();

        if (mbox == null)
            mbox = foldersName;
        folder = folder.getFolder(mbox);
        if (folder == null) {
            System.out.println("Invalid folder");
            System.exit(1);
        }

        try {
            // folder.open(Folder.READ_WRITE);
            folder.open(Folder.READ_ONLY);
        } catch (MessagingException ex) {
            folder.open(Folder.READ_ONLY);
        }
        int totalMessages = folder.getMessageCount();

        if (totalMessages == 0) {
            System.out.println("Empty folder");
            folder.close(false);
            store.close();
            System.exit(1);
        }

        if (verbose) {
            int newMessages = folder.getNewMessageCount();
            System.out.println("Total messages = " + totalMessages);
            System.out.println("New messages = " + newMessages);
            System.out.println("-------------------------------");
        }
        return folder;
    }

    private void setConnectionProp() {

        String[] argv = "-T, imaps, -H, outlook.office365.com, -U, yourUserName.com, -P, yourPassword".split(", ");

        // ============================= FOR GET SETTINGS FROM DB MAKE THIS:

        // String[] argv = getPropFromDB();

        // CREATE TABLE SETTING_EMAIL_IMPORT
        // (
        // SETTING_ID int PRIMARY KEY IDENTITY(1,1) NOT NULL,
        // COMPANY_NAME VARCHAR(20) NOT NULL,
        // USERNAME VARCHAR(30) NOT NULL,
        // PSSWRD VARCHAR(30) NOT NULL,
        // PROTOCOL VARCHAR(20) NOT NULL,
        // HOST VARCHAR(20) NOT NULL,
        // ADDR_DUBLICATE_JOB varchar(40), // email address for dublicate job
        // );
        //
        // INSERT INTO SETTING_EMAIL_IMPORT (COMPANY_NAME, USERNAME, PSSWRD, PROTOCOL, HOST, ADDR_DUBLICATE_JOB) VALUES
        // ('presidentialluxurylimo', 'reservations@presidentialluxurylimo.com', 'P@7187079999', 'imaps', 'outlook.office365.com',
        // 'reservations@presidentialluxurylimo.com')

        int optind;

        for (optind = 0; optind < argv.length; optind++) {
            if (argv[optind].equals("-T")) {
                protocol = argv[++optind];
            } else if (argv[optind].equals("-H")) {
                host = argv[++optind];
            } else if (argv[optind].equals("-U")) {
                user = argv[++optind];
            } else if (argv[optind].equals("-P")) {
                password = argv[++optind];
            } else if (argv[optind].equals("-v")) {
                verbose = true;
            } else if (argv[optind].equals("-D")) {
                debug = true;
            } else if (argv[optind].equals("-f")) {
                mbox = argv[++optind];
            } else if (argv[optind].equals("-L")) {
                url = argv[++optind];
            } else if (argv[optind].equals("-p")) {
                port = Integer.parseInt(argv[++optind]);
            } else if (argv[optind].equals("-s")) {
                showStructure = true;
            } else if (argv[optind].equals("-S")) {
                saveAttachments = true;
            } else if (argv[optind].equals("-m")) {
                showMessage = true;
            } else if (argv[optind].equals("-a")) {
                showAlert = true;
            } else if (argv[optind].equals("--")) {
                optind++;
                break;
            } else if (argv[optind].startsWith("-")) {
                System.out.println(
                        "Usage: msgshow [-L url] [-T protocol] [-H host] [-p port] [-U user]");
                System.out.println(
                        "\t[-P password] [-f mailbox] [msgnum ...] [-v] [-D] [-s] [-S] [-a]");
                System.out.println(
                        "or     msgshow -m [-v] [-D] [-s] [-S] [-f msg-file]");
                System.exit(1);
            } else {
                break;
            }
        }
    }

    private String[] getPropFromDB() {
        ArrayList<String> params = new ArrayList<String>();
        QueryDataSet qdsSettingsEmail = new QueryDataSet();
        ParameterRow pr = new ParameterRow();

        pr.addColumn("SETTING_ID", Variant.INT);
        pr.addColumn("COMPANY_NAME", Variant.STRING);
        pr.addColumn("USERNAME", Variant.STRING);
        pr.addColumn("PSSWRD", Variant.STRING);
        pr.addColumn("PROTOCOL", Variant.STRING);
        pr.addColumn("HOST", Variant.STRING);
        pr.addColumn("ADDR_DUBLICATE_JOB", Variant.STRING);
        qdsSettingsEmail.close();
        qdsSettingsEmail.setQuery(new QueryDescriptor(Connection.getNewAddressInstance(),
                "SELECT * FROM SETTING_EMAIL_IMPORT WHERE SETTING_ID = :SETTING_ID",
                pr, true));
        qdsSettingsEmail.getParameterRow().setInt("SETTING_ID", 2);
        qdsSettingsEmail.refresh();

        if (qdsSettingsEmail.isOpen()) {
            params.add("-v");
            // params.add("-D");
            params.add("-T");
            params.add(qdsSettingsEmail.getString("PROTOCOL"));
            params.add("-H");
            params.add(qdsSettingsEmail.getString("HOST"));
            params.add("-U");
            params.add(qdsSettingsEmail.getString("USERNAME"));
            user = qdsSettingsEmail.getString("USERNAME");
            params.add("-P");
            params.add(qdsSettingsEmail.getString("PSSWRD"));
            // params.add("-S");
            // params.add("-D");
            password = qdsSettingsEmail.getString("PSSWRD");
            ADDR_DUBLICATE_JOB = qdsSettingsEmail.getString("ADDR_DUBLICATE_JOB");
        }
        qdsSettingsEmail.close();

        return params.toArray(new String[params.size()]);
    }

    static Hashtable<String, String[]> templates = getTemplates();
    private static boolean saveAtt;
    private static long uid;

    private static void treateEmail(Message msg) {

        String sender = null;
        try {
            sender = msg.getFrom() == null ? null : ((InternetAddress) msg.getFrom()[0]).getAddress().toLowerCase();
        } catch (MessagingException e2) {
            e2.printStackTrace();
        }

        String className = "";
        Integer blngId = 0;
        String acctId = "";
        String affiliateEmail = "";
        Set<String> keys = templates.keySet();
        for (String key : keys)
            if (sender.contains(key.trim().toLowerCase())) {
                className = templates.get(key)[0].toUpperCase().replaceAll(" +", "").trim();
                blngId = Integer.valueOf(templates.get(key)[1]);
                acctId = templates.get(key)[2];
                affiliateEmail = templates.get(key)[3];
            }

        AffiliateJobsImportFrame.updateUI("for sender = " + sender + ", classname = " + className);
        saveAtt = false;
        if (className.equals("ALLTRANS") || className.equals("COMMUNICOR")) {
            AffiliateJobsImportFrame.updateUI("---- get pdf file from Communicor or AllTrans. ----");
            saveAtt = true;
        }

        contentForParse = "";
        contentHtml = "";
        try {
            dumpPart(msg);
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        if (className != null && !className.equals("")) {
            AffiliateJobsImportFrame.updateUI("Usually, we get job from this email.");
            try {
                Template template = (Template) Class.forName("com.expl.affImport.templates." + className).newInstance();
                if (template.checkSubject(msg.getSubject())) {
                    AffiliateJobsImportFrame.updateUI("Yes, I think it is job (subject looks like job). UID = " + uid);
                    AffiliateJobsImportFrame.updateUI("Company: " + className + ", Subject: " + msg.getSubject());

                    // We can get job from one email address from different Accounts. Only for Carey (careyreservations.noreply@carey.com).
                    if (msg.getSubject() != null && msg.getSubject() != "" && msg.getSubject().startsWith("Carey New York")) {
                        blngId = 5872; // account_display_id=829 Carey New York
                        acctId = "" + 829;
                        affiliateEmail = "";
                    }

                    String contentForSave = (contentHtml == null || contentHtml.trim().equals("")) ? contentForParse : contentHtml;
                    if (true) {
                        contentForSave = (saveAtt) ? "" + uid : contentForSave;

                        if (contentForParse.isEmpty())
                            contentForParse = ImportUtils.html2text(contentHtml);

                        ImportUtils.setStartInfo(msg.getSubject() + System.getProperty("line.separator") + contentForParse,
                                contentForSave, uid);

                        JobObj jobObj = new JobObj();
                        jobObj.setBlngCmpId(blngId);
                        jobObj.setAccountDispId(acctId);
                        jobObj.setEmailAddr(sender);

                        try {
                            template.saveJob(jobObj);
                        } catch (Exception e) {
                            AffiliateJobsImportFrame.updateUI("I'm sorry. Problem during the parse. But I try to save job.");
                            e.printStackTrace();
                        }

                        ImportUtils.saveJob(jobObj);
                    }
                } else
                    AffiliateJobsImportFrame.updateUI("Unfortunately it doesn't job. Subject doesn't looks like job. UID = " + uid);
            } catch (Exception e) {
                AffiliateJobsImportFrame.updateUI("We don't have class (template) for sender: " + sender + ". Please, check Class for name: " + className);
            }
        } else {
            AffiliateJobsImportFrame.updateUI("I think it is NOT job. Usually we don't get job from address: " + sender);
        }

    }

    private static Hashtable<String, String[]> getTemplates() {
        if (templates == null) {
            templates = new Hashtable<String, String[]>();

            QueryDataSet qdsTamplates = new QueryDataSet();
            ParameterRow pr2 = new ParameterRow();
            pr2.addColumn("EMAIL_FROM", Variant.STRING);
            pr2.addColumn("BLNG_ID", Variant.INT);

            pr2.addColumn("ACCT_DISPLAY_ID", Variant.STRING);
            pr2.addColumn("EMAIL_ADDR_TXT", Variant.STRING);

            qdsTamplates.close();
            qdsTamplates.setQuery(new QueryDescriptor(Connection.getNewAddressInstance(),
                    "select TEMPLATES_EMAIL_IMPORT.BLNG_ID, TEMPLATES_EMAIL_IMPORT.EMAIL_FROM, TEMPLATES_EMAIL_IMPORT.COMPANY_NAME, "
                            + "AFFILIATE.EMAIL_ADDR_TXT, ACCT.ACCT_DISPLAY_ID from templates_email_import join acct on TEMPLATES_EMAIL_IMPORT.BLNG_ID=acct.BLNG_ID "
                            + "LEFT OUTER JOIN AFFILIATE ON TEMPLATES_EMAIL_IMPORT.BLNG_ID=AFFILIATE.BLNG_ID",
                    pr2, true));
            qdsTamplates.refresh();

            if (qdsTamplates != null && qdsTamplates.isOpen() && qdsTamplates.getRowCount() > 0) {
                qdsTamplates.first();

                while (qdsTamplates.inBounds()) {

                    int blngId = qdsTamplates.getInt("BLNG_ID");

                    String acctDisplId = qdsTamplates.getString("ACCT_DISPLAY_ID");
                    String emailFromAfft = qdsTamplates.getString("EMAIL_ADDR_TXT");
                    String[] value = {qdsTamplates.getString("COMPANY_NAME"), String.valueOf(blngId), acctDisplId, emailFromAfft};

                    templates.put(qdsTamplates.getString("EMAIL_FROM").trim().toLowerCase(), value);
                    qdsTamplates.next();
                }
                qdsTamplates.close();
            }
        }

        return templates;
    }

    public static void dumpPart(Part p) throws Exception {

        if (saveAtt) {
            String attachFiles = "";
            if (Part.ATTACHMENT.equalsIgnoreCase(p.getDisposition())) {
                // this part is attachment
                String fileName = fileName;
                attachFiles += fileName + ", ";
                String pathForSave = "//db-main/expl/resources/Attachemnts_From_Email";
                try {
                    ((MimeBodyPart) p).saveFile(pathForSave + File.separator + uid + ".pdf");
                } catch (Exception e) {
                    AffiliateJobsImportFrame.updateUI("!!!! !!!! !!!! !!!! !!!! !!!! !!!! !!!! ");
                    AffiliateJobsImportFrame.updateUI("!!!! I DIDN'T SAVE PDF FILE !!!! UID = " + uid);
                    AffiliateJobsImportFrame.updateUI("PROBLEM: " + e.getMessage());
                    AffiliateJobsImportFrame.updateUI("path required: " + pathForSave);
                }
            }
            if (attachFiles.length() > 1) {
                attachFiles = attachFiles.substring(0, attachFiles.length() - 2);
            }
        }

        String ct = p.getContentType();
        try {
            pr("CONTENT-TYPE: " + (new ContentType(ct)).toString());
        } catch (ParseException pex) {
            pr("BAD CONTENT-TYPE: " + ct);
        }
        String filename = p.getFileName();
        if (filename != null)
            pr("FILENAME: " + filename);

        if (p.isMimeType("text/plain")) {
            pr("This is plain text");
            pr("---------------------------");
            contentForParse = (String) p.getContent();
            if (!showStructure && !saveAttachments) {
                contentForParse = (String) p.getContent();
            }
        } else if (p.isMimeType("multipart/*")) {
            pr("This is a Multipart");
            pr("---------------------------");
            Multipart mp = (Multipart) p.getContent();
            level++;
            int count = mp.getCount();
            for (int i = 0; i < count; i++)
                dumpPart(mp.getBodyPart(i));
            level--;
        } else if (p.isMimeType("message/rfc822")) {
            pr("This is a Nested Message");
            pr("---------------------------");
            level++;
            dumpPart((Part) p.getContent());
            level--;
        } else {
            if (!showStructure && !saveAttachments) {
                Object o = p.getContent();
                if (o instanceof String) {
                    pr("This is a string");
                    pr("---------------------------");
                    contentHtml = (String) o;
                } else if (o instanceof InputStream) {

                    pr("This is just an input stream");
                    pr("---------------------------");
                } else {
                    pr("This is an unknown type");
                    pr("---------------------------");
                    pr(o.toString());
                }
            } else {
                pr("---------------------------");
            }
        }

        if (saveAttachments && level != 0 && p instanceof MimeBodyPart &&
                !p.isMimeType("multipart/*")) {
            String disp = p.getDisposition();
            if (disp == null || disp.equalsIgnoreCase(Part.ATTACHMENT)) {
                if (filename == null)
                    filename = "Attachment" + attnum++;
                pr("Saving attachment to file " + filename);
                try {
                    File f = new File(filename);
                    if (f.exists())
                        throw new IOException("file exists");
                    ((MimeBodyPart) p).saveFile(f);
                } catch (IOException ex) {
                    pr("Failed to save attachment: " + ex);
                }
                pr("---------------------------");
            }
        }
    }

    public static void dumpEnvelope(Message m) throws Exception {
        pr("This is the message envelope");
        pr("---------------------------");
        Address[] a;
        // FROM
        if ((a = m.getFrom()) != null) {
            for (int j = 0; j < a.length; j++)
                pr("FROM: " + a[j].toString());
        }

        // REPLY TO
        if ((a = m.getReplyTo()) != null) {
            for (int j = 0; j < a.length; j++)
                pr("REPLY TO: " + a[j].toString());
        }

        // TO
        if ((a = m.getRecipients(Message.RecipientType.TO)) != null) {
            for (int j = 0; j < a.length; j++) {
                pr("TO: " + a[j].toString());
                InternetAddress ia = (InternetAddress) a[j];
                if (ia.isGroup()) {
                    InternetAddress[] aa = ia.getGroup(false);
                    for (int k = 0; k < aa.length; k++)
                        pr("  GROUP: " + aa[k].toString());
                }
            }
        }

        // SUBJECT
        pr("SUBJECT: " + m.getSubject());

        // DATE
        Date d = m.getSentDate();
        pr("SendDate: " +
                (d != null ? d.toString() : "UNKNOWN"));

        // FLAGS
        Flags flags = m.getFlags();
        StringBuffer sb = new StringBuffer();
        Flags.Flag[] sf = flags.getSystemFlags(); // get the system flags

        boolean first = true;
        for (int i = 0; i < sf.length; i++) {
            String s;
            Flags.Flag f = sf[i];
            if (f == Flags.Flag.ANSWERED)
                s = "\\Answered";
            else if (f == Flags.Flag.DELETED)
                s = "\\Deleted";
            else if (f == Flags.Flag.DRAFT)
                s = "\\Draft";
            else if (f == Flags.Flag.FLAGGED)
                s = "\\Flagged";
            else if (f == Flags.Flag.RECENT)
                s = "\\Recent";
            else if (f == Flags.Flag.SEEN)
                s = "\\Seen";
            else
                continue; // skip it
            if (first)
                first = false;
            else
                sb.append(' ');
            sb.append(s);
        }

        String[] uf = flags.getUserFlags(); // get the user flag strings
        for (int i = 0; i < uf.length; i++) {
            if (first)
                first = false;
            else
                sb.append(' ');
            sb.append(uf[i]);
        }
        pr("FLAGS: " + sb.toString());

        // X-MAILER
        String[] hdrs = m.getHeader("X-Mailer");
        if (hdrs != null)
            pr("X-Mailer: " + hdrs[0]);
        else
            pr("X-Mailer NOT available");
    }

    static String indentStr = "                                               ";
    static int level = 0;

    /**
     * Print a, possibly indented, string.
     */
    public static void pr(String s) {
        if (showStructure)
            System.out.print(indentStr.substring(0, level * 2));
    }

    private static long getUIDLastMail() {
        Column colJobId = new Column();
        Column colWebSrvcReqTxt = new Column();

        colJobId.setColumnName("JOB_ID");
        colJobId.setServerColumnName("JOB_ID");
        colJobId.setDataType(com.borland.dx.dataset.Variant.INT);

        colWebSrvcReqTxt.setColumnName("EMAIL_UID");
        colWebSrvcReqTxt.setServerColumnName("EMAIL_UID");
        colWebSrvcReqTxt.setDataType(com.borland.dx.dataset.Variant.LONG);

        QueryDataSet dataSet = new QueryDataSet();
        dataSet.setColumns(new Column[]{colJobId, colWebSrvcReqTxt});
        dataSet.setQuery(new QueryDescriptor(UniversalMBox.DB,
                "SELECT * FROM IMPORT_JOB_FROM_EMAIL WHERE JOB_ID = (SELECT MAX(JOB_ID) FROM IMPORT_JOB_FROM_EMAIL)"));
        if (!dataSet.isOpen()) dataSet.open();
        long uid = dataSet.getLong("EMAIL_UID");
        dataSet.close();

        return uid;
    }

    public static boolean sendEmailDublicateJob(String messageBody) {

        String EMAIL_SMTP_PASSWORD = "Legend88";
        String EMAIL_SMTP_SERVER = "smtp.office365.com";
        String EMAIL_SMTP_USER = "reservations@legendslimousine.com";

        Email email = new SimpleEmail();

        email.setHostName("smtp.office365.com");
        email.setSmtpPort(587);
        email.setAuthenticator(new DefaultAuthenticator(EMAIL_SMTP_USER, EMAIL_SMTP_PASSWORD));
        email.setStartTLSEnabled(true);
        // email.setSSLOnConnect(true);
        try {
            email.setFrom(EMAIL_SMTP_USER);
            email.setSubject("Test");
            email.setDebug(true);
            email.setMsg("This is a test mail ... :-)");
            email.addTo("sadkoua@gmail.com");
            email.send();
        } catch (EmailException e) {
            e.printStackTrace();
        }

        final String user = EMAIL_SMTP_USER;
        final String password = EMAIL_SMTP_PASSWORD;
        messageBody = "Hi";
        Properties props = new Properties();
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.host", EMAIL_SMTP_SERVER);

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(user, password);
                    }
                });
        session.setDebug(true);

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("your@address.com"));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse("vlad@gmail.com"));

            message.setSubject("For Tim!");
            message.setText(messageBody);

            Transport.send(message);

            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

}
