package org.basex.query.fs;

import static org.basex.Text.NL;
import java.io.IOException;
import org.basex.core.Context;
import org.basex.data.Data;
import org.basex.io.PrintOutput;
import org.basex.util.GetOpts;
import org.basex.util.IntList;
import org.basex.util.Token;


/**
 * Performs a ls command.
 * 
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Hannes Schwarz - Hannes.Schwarz@gmail.com
 */
public final class LS {

  /** BaseX table. */
  private final Data data;

  /** current dir. */
  private int curDirPre;

  /** PrintOutPutStream. */
  private PrintOutput out;

  /** list subdirectories also. */
  private boolean fRecursive;

  /** list files beginning with . */
  private boolean fListDot;

  /** print long version. */
  private boolean fPrintLong;

  /** Shows if an error occurs. */
  private boolean fError;

  /**
   * Simplified Constructor.
   * @param ctx data context
   * @param output output stream
   */
  public LS(final Context ctx, final PrintOutput output) {
    data = ctx.data();
    curDirPre = ctx.current().pre[0];
    this.out = output;
  }

  /**
   * Performs an ls command.
   * 
   * @param cmd - command line
   * @throws IOException - in case of problems with the PrintOutput 
   */
  public void lsMain(final String cmd) 
  throws IOException {    
    GetOpts g = new GetOpts(cmd, "ahlR", 1);

    // get all Options
    int ch = g.getopt();
    while (ch != -1) {
      switch (ch) {
        case 'R':
          fRecursive = true;
          break;
        case 'a':
          fListDot = true;
          break;
        case 'h':
          printHelp();
          return;
        case 'l':
          fPrintLong = true; 
          break;
        case ':':         
          fError = true;
          out.print("ls: missing argument");
          return;  
        case '?':         
          fError = true;
          out.print("ls: illegal option");
          return;
      }
      if(!fError) {
        ch = g.getopt();
      }
    }
    // if there is path expression set new pre value
    if(g.getPath() != null) {
      curDirPre = FSUtils.goToDir(data, curDirPre, g.getPath());
      if(curDirPre == -1)
        out.print("ls " + g.getPath() + "No such file or directory. ");
    }

    // go to work
    if(fRecursive) {
      lsRecursive(curDirPre);
    } else {
      print(FSUtils.getAllOfDir(data, curDirPre));
    }    
  }

  /**
   * Recursively list subdirectories encountered.
   *  
   * @param pre Value of dir 
   * @throws IOException in case of problems with the PrintOutput 
   */
  private void lsRecursive(final int pre) throws IOException {         

    int[] contentDir = FSUtils.getAllOfDir(data, pre);  
    int[] allDir = print(contentDir);   

    for(int i = 0; i < allDir.length; i++) { 
      if(!fListDot) {    
        // don´t crawl dirs starting with ´.´
        byte[] name = FSUtils.getName(data, allDir[i]);
        if(Token.startsWith(name, '.'))
          continue;
      }
      out.print(NL);
      out.print(FSUtils.getPath(data, allDir[i]));      
      out.print(NL);
      out.flush();
      lsRecursive(allDir[i]); 
    }
  }

  /**
   * Print the result.
   * @param result - array to print
   * @return list of directories found 
   */
  private int[] printLong(final int[] result) {
    IntList allDir = new IntList();
    for(int j : result) {
      if(FSUtils.isDir(data, j)) allDir.add(j);      
      byte[] name = FSUtils.getName(data, j);
      long size = FSUtils.getSize(data, j);
      byte[] time = FSUtils.getMtime(data, j);
      char file = 'd';
      if(FSUtils.isFile(data, j))
        file = 'f';
      if(!fListDot) {
        // do not print files starting with .
        if(Token.startsWith(name, '.'))
          continue;
      }      
      System.out.printf("%-3s %-30s %10s %20s\n", file, 
          Token.string(name), size, Token.string(time));
    }
    return allDir.finish();
  }


  /**
   * Print the result.
   * @param result - array to print
   * @throws IOException in case of problems with the PrintOutput
   * @return list of directories found
   */
  private int[] print(final int[] result) throws IOException {
        
    if(fPrintLong) {
      return printLong(result);
    } else {
      IntList allDir = new IntList();      
      for(int j : result) {
        if(FSUtils.isDir(data, j)) allDir.add(j);
        byte[] name = FSUtils.getName(data, j);
        if(!fListDot) {
          // do not print files starting with .
          if(Token.startsWith(name, '.'))
            continue;
        }
        out.print(name);
        out.print("\t");        
      }      
      out.print(NL);
      return allDir.finish();
    }
  }
  /**
   * Print the help.
   * 
   * @throws IOException in case of problems with the PrintOutput
   */
  private void printHelp() throws IOException {
    out.print("ls -ahR ...");

  }
}
