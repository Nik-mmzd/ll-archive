package com.turikhay.tlauncher.ui;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.turikhay.tlauncher.TLauncher;
import com.turikhay.tlauncher.settings.Settings;
import com.turikhay.tlauncher.util.AsyncThread;
import com.turikhay.tlauncher.util.U;

public class Alert {
	private static boolean show = false;
	private static final int wrap = 90;
	
	private static Settings lang;
	private static String
		PREFIX = "TLauncher : ",
		DEFAULT_TITLE = "An error occurred",
		DEFAULT_MESSAGE = "An unexpected error occurred",
		MISSING_TITLE = "MISSING TITLE",
		MISSING_MESSAGE = "MISSING MESSAGE";
	private static final boolean DEFAULT_EXIT = false;
	
	public static void showError(String title, String message, Object textarea, Throwable e, boolean exit){
		if(show) return; show = true;
		JFrame frame = new JFrame();
		String 	t_title = PREFIX + title,
				t_message = (message != null)? U.w("<html><div align=\"justify\">" + U.w(message, wrap).replace("\n", "<br/>") + "</div></html>", wrap) : null,
				t_throwable =  ((e!=null)? U.stackTrace(e) : null),
				t_textarea = (textarea != null)? textarea.toString() : null;
		
		AlertPanel panel = new AlertPanel(t_message);
		if(t_textarea != null) panel.addTextArea(t_textarea);
		if(t_throwable != null) panel.addTextArea(t_throwable);
		
		frame.requestFocus();
		JOptionPane.showMessageDialog(frame, panel, t_title, JOptionPane.ERROR_MESSAGE);
		
		show = false;
		if(exit) System.exit(1);
	}
	public static void showError(String title, String message, Throwable e){ showError(title, message, null, e, DEFAULT_EXIT); }
	public static void showError(String message, Throwable e){ showError(DEFAULT_TITLE, message, null, e, DEFAULT_EXIT); }
	public static void showError(Throwable e, boolean exit ){ showError(DEFAULT_TITLE, DEFAULT_MESSAGE, null, e, exit); }
	
	public static void showError(String title, String message, Object textarea){ showError(title, message, textarea, null, DEFAULT_EXIT); }
	public static void showError(String title, String message){ showError(title, message, null, null, DEFAULT_EXIT); }
	
	public static void showWarning(String title, String message){
		if(show) return; show = true;
		JFrame frame = new JFrame();
		String 	t_title = PREFIX + title, t_message = U.w(message, wrap);
		
		frame.requestFocus();
		JOptionPane.showMessageDialog(frame, t_message, t_title, JOptionPane.WARNING_MESSAGE);
		
		show = false;
	}
	public static void showAsyncWarning(final String title, final String message){ AsyncThread.execute(new Runnable(){public void run(){ showWarning(title, message); }}); }
	public static void showWarning(String path){ showWarning(getLocal(path + ".title", MISSING_TITLE), getLocal(path, MISSING_MESSAGE)); }
	
	public static boolean showQuestion(String title, String message, Object textarea, boolean force){
		if(!force && show) return false; show = true;
		JFrame frame = new JFrame();
		String 	t_title = PREFIX + title,
				t_message = (message != null)? U.w("<html><div align=\"justify\">" + U.w(message, wrap).replace("\n", "<br/>") + "</div></html>", wrap) : null,
				t_textarea = (textarea != null)? textarea.toString() : null;
				
		AlertPanel panel = new AlertPanel(t_message);
		if(t_textarea != null) panel.addTextArea(t_textarea);
		
		frame.requestFocus();
		boolean result = (JOptionPane.showConfirmDialog(frame, panel, t_title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
		
		show = false;
		return result;
	}
	public static boolean showQuestion(String path, Object textarea, boolean force){ return showQuestion(getLocal(path + ".title", MISSING_TITLE), getLocal(path, MISSING_MESSAGE), textarea, force); }
	public static boolean showQuestion(String path, boolean force){ return showQuestion(path, null, force); }
	
	public static void showMessage(String title, String message, Object textarea){
		if(show) return; show = true;
		JFrame frame = new JFrame();
		String 	t_title = PREFIX + title,
				t_message = (message != null)? U.w("<html><div align=\"justify\">" + U.w(message, wrap).replace("\n", "<br/>") + "</div></html>", wrap) : null,
				t_textarea = (textarea != null)? textarea.toString() : null;
		
		AlertPanel panel = new AlertPanel(t_message);
		if(t_textarea != null) panel.addTextArea(t_textarea);
		
		frame.requestFocus();
		JOptionPane.showMessageDialog(frame, panel, t_title, JOptionPane.INFORMATION_MESSAGE);
		
		show = false;
	}
	public static void showMessage(String path, Object textarea){ showMessage(getLocal(path + ".title", MISSING_TITLE), getLocal(path, MISSING_MESSAGE), textarea); }
	
	private static String getLocal(String path, String message){
		try{
			if(lang == null) lang = TLauncher.getInstance().getLang();
			return lang.get(path);
		}catch(Throwable e){ e.printStackTrace(); }
		
		return message;
	}
	
	public static void prepareLocal(){
		DEFAULT_TITLE = getLocal("alert.error.title", DEFAULT_TITLE);
		DEFAULT_MESSAGE = getLocal("alert.error.message", DEFAULT_MESSAGE);
	}
	
}
