package net.minecraft.launcher_.process;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.launcher_.OperatingSystem;

public class JavaProcessLauncher
{
  private final String jvmPath;
  private final List<String> commands;
  private File directory;

  public JavaProcessLauncher(String jvmPath, String[] commands)
  {
    if (jvmPath == null) jvmPath = OperatingSystem.getCurrentPlatform().getJavaDir();
    this.jvmPath = jvmPath;
    this.commands = new ArrayList<String>();
    Collections.addAll(this.commands, commands);
  }

  public JavaProcess start() throws IOException {
    List<String> full = getFullCommands();
    return new JavaProcess(full, new ProcessBuilder(full).directory(this.directory).redirectErrorStream(true).start());
  }

  public List<String> getFullCommands() {
    List<String> result = new ArrayList<String>(this.commands);
    result.add(0, getJavaPath());
    return result;
  }

  public List<String> getCommands() {
    return this.commands;
  }
  
  public void addCommand(Object command){
	  this.commands.add(command.toString());
  }
  
  public void addCommand(Object key, Object value){
	  this.commands.add(key.toString());
	  this.commands.add(value.toString());
  }

  public void addCommands(Object[] commands) {
	  for(Object c : Arrays.asList(commands))
		  this.commands.add(c.toString());
  }

  public void addSplitCommands(Object commands) {
    addCommands(commands.toString().split(" "));
  }

  public JavaProcessLauncher directory(File directory) {
    this.directory = directory;

    return this;
  }

  public File getDirectory() {
    return this.directory;
  }

  protected String getJavaPath() {
    return this.jvmPath;
  }

  public String toString()
  {
    return "JavaProcessLauncher[commands=" + this.commands + ", java=" + this.jvmPath + "]";
  }
}