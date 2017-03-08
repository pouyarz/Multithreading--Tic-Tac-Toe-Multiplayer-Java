// Fig. 21.7: TicTacToeServer.java
// This class maintains a game of Tic-Tac-Toe for two
// client applets.
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

public class TicTacToeServer extends JFrame {
   private byte board[];
   private boolean xMove;
   private JTextArea output;
   public Player players[];
   private ServerSocket server;
   private int currentPlayer;
   public byte winner;
   

   public TicTacToeServer()
   {
      super( "Tic-Tac-Toe Server" );
      //pnlButton
      board = new byte[ 9 ];
      xMove = true;
      players = new Player[ 2 ];
      currentPlayer = 0;
 
      // set up ServerSocket
      try {
         server = new ServerSocket( 5000, 2 );
      }
      catch( IOException e ) {
         e.printStackTrace();
         System.exit( 1 );
      }
      
      output = new JTextArea();
      getContentPane().add( output, BorderLayout.CENTER );
      output.setText( "Server awaiting connections\n" );

      setSize( 200, 200 );
      show();
   }

   // wait for two connections so game can be played
   public void execute()
   {
      for ( int i = 0; i < players.length; i++ ) {
         try {
            players[ i ] =
               new Player( server.accept(), this, i );
            players[ i ].start();
         }
         catch( IOException e ) {
            e.printStackTrace();
            System.exit( 1 );
         }
      }

      // Player X is suspended until Player O connects.
      // Resume player X now.          
      synchronized ( players[ 0 ] ) {
         players[ 0 ].threadSuspended = false;   
         players[ 0 ].notify();
      }
  
   }
   
   public void display( String s )
   {
      output.append( s + "\n" );
   }
   
   public void restart()
   {
       for (int i = 0; i < 9; i++) {
           board[i] = ' ';
       }
       try {
           
           players[0].output.writeUTF( "restart" );
           players[1].output.writeUTF( "restart" );
       } catch (IOException ex) {
           Logger.getLogger(TicTacToeServer.class.getName()).log(Level.SEVERE, null, ex);
       }
   }
   
 
   // Determine if a move is valid.
   // This method is synchronized because only one move can be
   // made at a time.
   public synchronized boolean validMove( int loc,
                                          int player )
   {
      boolean moveDone = false;

      while ( player != currentPlayer ) {
         try {
            wait();
         }
         catch( InterruptedException e ) {
            e.printStackTrace();
         }
      }

      if ( !isOccupied( loc ) ) {
         board[ loc ] =
            (byte) ( currentPlayer == 0 ? 'X' : 'O' );
         currentPlayer = ( currentPlayer + 1 ) % 2;
         players[ currentPlayer ].otherPlayerMoved( loc );
         notify();    // tell waiting player to continue
         return true;
      }
      else 
         return false;
   }

   public boolean isOccupied( int loc )
   {
      if ( board[ loc ] == 'X' || board [ loc ] == 'O' )
          return true;
      else
          return false;
   }

   public boolean gameOver()
   {
        if (test(board[0], board[1], board[2]))
            return true;
	if (test(board[3], board[4], board[5]))
	    return true;
	if (test(board[6], board[7], board[8]))
	    return true;
	if (test(board[0], board[3], board[6]))
	    return true;
	if (test(board[1], board[4], board[7]))
	    return true;
	if (test(board[2], board[5], board[8]))
	    return true;
	if (test(board[0], board[4], board[8]))
	    return true;

	return false;
    }

    private boolean test(byte x, byte y, byte z){
	if (((x==y)&&(y == z))&&((x =='X')||(x =='O'))){
            winner = x;
	    return true;
	}
	else{
	    return false;
	}
    }
	

   public static void main( String args[] )
   {
      TicTacToeServer game = new TicTacToeServer();

      game.addWindowListener( new WindowAdapter() {
        public void windowClosing( WindowEvent e )
            {
               System.exit( 0 );
            }
         }
      );

      game.execute();
   }
}

// Player class to manage each Player as a thread
class Player extends Thread {
   private Socket connection;
   private DataInputStream input;
   public DataOutputStream output;
   private TicTacToeServer control;
   private int number;
   private char mark;
   protected boolean threadSuspended = true;

   public Player( Socket s, TicTacToeServer t, int num )
   {
      mark = ( num == 0 ? 'X' : 'O' );

      connection = s;
      
      try {
         input = new DataInputStream(
                    connection.getInputStream() );
         output = new DataOutputStream(
                    connection.getOutputStream() );
      }
      catch( IOException e ) {
         e.printStackTrace();
         System.exit( 1 );
      }

      control = t;
      number = num;
   }

   public void otherPlayerMoved( int loc )
   {
      try {
         output.writeUTF( "Opponent moved" );
         output.writeInt( loc );
      }
      catch ( IOException e ) { e.printStackTrace(); }
   }
   
    
  
   public void run()
   {
      boolean done = false;
      
      try {
         control.display( "Player " +
            ( number == 0 ? 'X' : 'O' ) + " connected" );
         output.writeChar( mark );
         output.writeUTF( "Player " +
            ( number == 0 ? "X connected\n" :
                            "O connected, please wait\n" ) );

         // wait for another player to arrive
         
         if ( mark == 'X' ) {
            output.writeUTF( "Waiting for another player" );

            try {
               synchronized( this ) {   
                  while ( threadSuspended )
                     wait();  
               }
            } 
            catch ( InterruptedException e ) {
               e.printStackTrace();
            }

            output.writeUTF(
               "Other player connected. Your move." );
         }

         // Play game
         while ( !done ) {
            int location = input.readInt();
            if ( location == -1 )
            {
                control.restart();
            }
            else if ( control.validMove( location, number ) ) {
                control.display( "loc: " + location );
                output.writeUTF( "Valid move." );
            }
            else 
               output.writeUTF( "Invalid move, try again" );

            if ( control.gameOver() )
            {
                control.display("Winner: "+ (char)control.winner + "\n");
                control.players[0].output.writeUTF("Winner: "+ (char)control.winner);
                control.players[1].output.writeUTF("Winner: "+ (char)control.winner);
               control.restart();
               //done = true;
            }
         }         
         
         connection.close();
      }
      catch( IOException e ) {
         e.printStackTrace();
         System.exit( 1 );
      }
   }
}                                                            



/**************************************************************************
 * (C) Copyright 1999 by Deitel & Associates, Inc. and Prentice Hall.     *
 * All Rights Reserved.                                                   *
 *                                                                        *
 * DISCLAIMER: The authors and publisher of this book have used their     *
 * best efforts in preparing the book. These efforts include the          *
 * development, research, and testing of the theories and programs        *
 * to determine their effectiveness. The authors and publisher make       *
 * no warranty of any kind, expressed or implied, with regard to these    *
 * programs or to the documentation contained in these books. The authors *
 * and publisher shall not be liable in any event for incidental or       *
 * consequential damages in connection with, or arising out of, the       *
 * furnishing, performance, or use of these programs.                     *
 *************************************************************************/
