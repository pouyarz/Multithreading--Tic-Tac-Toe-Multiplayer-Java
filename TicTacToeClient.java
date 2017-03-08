// Fig. 21.8: TicTacToeClient.java
// Client for the TicTacToe program
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

// Client class to let a user play Tic-Tac-Toe with
// another user across a network.
public class TicTacToeClient extends JApplet
                             implements Runnable {
   private JTextField id;
   private JTextArea display;
   private JPanel boardPanel, panel2;
   private Square board[][], currentSquare;
   private Socket connection;
   private DataInputStream input;
   private DataOutputStream output;
   private Thread outputThread;
   private char myMark;
   private boolean myTurn;

   // Set up user-interface and board
   public void init()
   {
      display = new JTextArea( 4, 30 );
      display.setEditable( false );
      getContentPane().add( new JScrollPane( display ),
                            BorderLayout.SOUTH );

      boardPanel = new JPanel();
      GridLayout layout = new GridLayout( 3, 3, 0, 0 );
      boardPanel.setLayout( layout );

      board = new Square[ 3 ][ 3 ];
      
      

      // When creating a Square, the location argument to the
      // constructor is a value from 0 to 8 indicating the
      // position of the Square on the board. Values 0, 1,
      // and 2 are the first row, values 3, 4, and 5 are the
      // second row. Values 6, 7, and 8 are the third row.
      for ( int row = 0; row < board.length; row++ )
      {
         for ( int col = 0;
                   col < board[ row ].length; col++ ) {
            board[ row ][ col ] =
               new Square( ' ', row * 3 + col );
            board[ row ][ col ].addMouseListener(
               new SquareListener(
                  this, board[ row ][ col ] ) );

            boardPanel.add( board[ row ][ col ] );        
         }
      }

      id = new JTextField();
      id.setEditable( false );
      
      getContentPane().add( id, BorderLayout.NORTH );
      
      panel2 = new JPanel();
      panel2.add( boardPanel, BorderLayout.CENTER );
      JButton p = new JButton("Restart");
      p.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {  
                try {
                    output.writeInt( -1 );
                } catch (IOException ex) {
                    Logger.getLogger(TicTacToeClient.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
      panel2.add(p);
      getContentPane().add( panel2, BorderLayout.CENTER );
   }

   // Make connection to server and get associated streams.
   // Start separate thread to allow this applet to
   // continually update its output in text area display.
   public void start()
   {
      try {
         connection = new Socket(
            InetAddress.getByName( "127.0.0.1" ), 5000 );
         input = new DataInputStream(
                        connection.getInputStream() );
         output = new DataOutputStream(
                        connection.getOutputStream() );
      }
      catch ( IOException e ) {
         e.printStackTrace();         
      }

      outputThread = new Thread( this );
      outputThread.start();
   }

   // Control thread that allows continuous update of the
   // text area display.
   public void run()
   {
      // First get player's mark (X or O)
      try {
         myMark = input.readChar();
         id.setText( "You are player \"" + myMark + "\"" );
         myTurn = ( myMark == 'X' ? true  : false );
      }
      catch ( IOException e ) {
         e.printStackTrace();         
      }

      // Receive messages sent to client
      while ( true ) {
         try {
            String s = input.readUTF();
            processMessage( s );
         }
         catch ( IOException e ) {
            e.printStackTrace();         
         }
      }
   }

   // Process messages sent to client
   public void processMessage( String s )
   {
      if ( s.equals( "Valid move." ) ) {
         display.append( "Valid move, please wait.\n" );
         currentSquare.setMark( myMark );
         currentSquare.repaint();
      }
      else if ( s.equals( "Invalid move, try again" ) ) {
         display.append( s + "\n" );
         myTurn = true;
      }
      else if ( s.equals( "Opponent moved" ) ) {
         try {
            int loc = input.readInt();
 
            board[ loc / 3 ][ loc % 3 ].setMark(
                  ( myMark == 'X' ? 'O' : 'X' ) );
            board[ loc / 3 ][ loc % 3 ].repaint();
                 
            display.append(
               "Opponent moved. Your turn.\n" );
            myTurn = true;
         }
         catch ( IOException e ) {
            e.printStackTrace();         
         }
      }
      else if(s.equals("restart"))
      {
           for ( int row = 0; row < board.length; row++ )
      {
         for ( int col = 0;
                   col < board[ row ].length; col++ ) {
            board[ row ][ col ].setMark(' ');
            board[ row ][ col ].repaint();
                    }
      }
           myTurn = myMark == 'X';
           display.append( s + "\n" );
      }
      else
         display.append( s + "\n" );

      display.setCaretPosition(
         display.getText().length() );
   }

   public void sendClickedSquare( int loc )
   {
      if ( myTurn )
         try {
            output.writeInt( loc );
            myTurn = false;
         }
         catch ( IOException ie ) {
            ie.printStackTrace();         
         }
   }

   public void setCurrentSquare( Square s )
   {
      currentSquare = s;
   }
}

// Maintains one square on the board
class Square extends JPanel {
   private char mark;
   private int location;

   public Square( char m, int loc)
   {
      mark = m;
      location = loc;
      setSize ( 30, 30 );
      
      setVisible(true);
   }

   public Dimension getPreferredSize() { 
      return ( new Dimension( 30, 30 ) );
   }

   public Dimension getMinimumSize() {
      return ( getPreferredSize() );
   }

   public void setMark( char c ) { mark = c; }

   public int getSquareLocation() { return location; }

   public void paintComponent( Graphics g )
   {
      super.paintComponent( g );
      g.drawRect( 0, 0, 29, 29 );
      g.drawString( String.valueOf( mark ), 11, 20 );   
   }
}

class SquareListener extends MouseAdapter {
   private TicTacToeClient applet;
   private Square square;

   public SquareListener( TicTacToeClient t, Square s )
   {
      applet = t;
      square = s;
   }

   public void mouseReleased( MouseEvent e )
   {
      applet.setCurrentSquare( square );
      applet.sendClickedSquare( square.getSquareLocation() );
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
