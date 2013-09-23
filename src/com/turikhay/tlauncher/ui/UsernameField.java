package com.turikhay.tlauncher.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UsernameField extends LocalizableTextField {
	private static final long serialVersionUID = -5813187607562947592L;
	String username;
	
	UsernameField(final LoginForm lf, String username, String placeholder, int columns){
		super(lf, placeholder, username, columns);
		
		this.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) { lf.callLogin(); }
		});
	}
	
	public boolean check(){
		return check(true);
	}
	
	protected boolean check(String text) {
		return check(text, true);
	}
	
	public boolean check(boolean canBeEmpty){
		String text = this.getValue();
		
		if(check(text, canBeEmpty))
			return ok();
		return wrong(l.get("username.incorrect"));
	}
	
	protected boolean check(String text, boolean canBeEmpty){
		if(text == null) return false;
		
		String regexp = "^[A-Za-z0-9_-]"+ ((canBeEmpty)? "*" : "+")  +"$";
		if(text.matches(regexp)){
			username = text;
			return true;
		}
		
		return false;
	}

}
