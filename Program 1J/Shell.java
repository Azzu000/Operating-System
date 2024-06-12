/*
Author: Rui qi Huang
Date: 1/11/2024
---------------------------------------------------------------------------------------
Description:
This is the shell class of our operating system. It will take in a user's command and execute it accordingly. For now only the PingPong
class can be initiated using the shell. When a ";" is inserted between commands, we expect the commands to be executed asynchronously.
When a "&" is inserted between commands, we expect the commands to be executed synchronously. When a exit command is initiated we exit
the shell and go back to the loader. When enter is press another shell will be loaded.

Assumptions:
We assume that the syntax of our commands given such as "PingPong abc 100" is correctly formatted. We also assume that the user is
only calling PingPong and not any other class. exit and the Enter key should be the only other commands accepted.

Note: Some of the code in run() is taken from the class lecture. You can see class lecture for
Chapter 2 slide 17 for the code. The idea of the function generateCmd was also taken from the given lecture slide but the code itself was not
since it was not given.
 */

 import java.io.*;
 import java.util.*;
 class Shell extends Thread {
     public Shell() { //Auto-generated Constructor
     }
 
     /*run()
     -------------------------------------------------------------------------
     Description: This function is the main function that will run the shell commands given. It will use SysLib to execute
     and join commands such as PingPong abc 4. After a command is ran, the Enter button needs to be pressed in order for another
     shell to appear.
     Pre-condition: There is no specific input argument for the run function but run takes the command line argument inputted by the user and processes it.
     In order for the run function to work properly we need to make sure the command inputted by the user is valid. Valid command inputs include:
     *PingPong args[1] args[2]
     *exit
     *Enter key is pressed
     Post-condition: The run function does not have a specific output that it returns. However, the run function utilizes SysLib to output any messages
     or even the shell to the system. It does also process the commands and execute the commands as follow. The output of these commands depends solely on
     the class that was called by the command or the command itself.
 
      */
     public void run() {
         //run will tell us whether the shell is going to continue running or not
         boolean run = true;
         //the first shell number starts at 1
         int line = 1;
         while (run == true) {
             //this will hold the command line arguments inputted by the user
             String cmdLine = "";
             while (cmdLine.length() == 0) {
                 StringBuffer inputBuf = new StringBuffer();
                 SysLib.cerr("shell[" + line + "]% ");
                 SysLib.cin(inputBuf);
                 cmdLine = inputBuf.toString();
             }
 
             //Converts the cmdLine into a string array
             String[] args = SysLib.stringToArgs(cmdLine);
             //first is the first element taken from our command, it should be PingPong or exit or null if Enter key is pressed
             int first = 0;
 
             for (int i = 0; i < args.length; i++) {
                 //Find one full command by finding ; or & at the end since all command ends with that
                 if(args[i].equals(";") || args[i].equals("&") || i == args.length - 1){
                     //command is going to hold the command we need to execute
                     String[] command = generateCmd(args, first, i);
                     if(command[0] == "x"){
                         //if the first command is
                         SysLib.cerr("Input not accepted. \n");
                         break;
                     }else if(command != null){
                         //if the commadn is exit then we exit the shell
                         if(command[0] == "exit"){
                             System.out.print("Exiting Shell..." + "\n");
                             SysLib.exit();
                             run = false;
                             return;
                         }
 
                         //executed is going to tell us if the command got executed or not
                         boolean executed = false;
 
                         //childID is going to save the child ID if the command is successfully executed
                         int childID = SysLib.exec(command);
 
                         //if the childID is equal to -1 then the execution failed otherwise the execution was successful
                         if(childID == -1){
                             executed = false;
                         }
                         else {
                             executed = true;
                         }
 
                         //These print statements is for testing purposes only but is also kept for informative purposes.
                         System.out.print("Found " + args[i] + "\n");
                         System.out.print("executed is " + executed + ", \n");
 
                         //if args[i]="&" don't call SysLib.join(), Otherwise (i.e., ";"), keep calling SysLib.join()
                         if(executed == true && args[i].equals(";")) {
 
                             //using a while loop to keep calling SysLib.join() since it is ";"
                             int join = SysLib.join();
 
                             //if the join is not equal to the childID that means that we are still waiting to be joined, so we keep joining until join is successful.
                             while(join != childID){
                                join = SysLib.join();
 
                                //if join is -1 that means the join have failed, so we send out an error
                                 if( join == -1){
                                     SysLib.cerr("Join failed. \n");
                                 }
                             }
                         }
                     }
                     else {
                         //if command is equal to null or the Enter key was pressed then we break from the loop
                         SysLib.cerr("Input not accepted. \n");
                         break;
                     }
                     //go on to the next command delimited
                     first = i + 1;
                 }
             }
             line = line + 1;
         }
 
     }
 
     /*String[] generateCmd(String[] command, int firstElement, int lastElement)
     ------------------------------------------------------------------
     Description: Given the commands firstElement index, which in this case is PingPong or exit, and last Element index,
     which is either "&" or ";", and derive a command string which contains the full command such as PingPong abc 4 or exit.
     Will return a string array of such command. This class helps derive the command so we can execute the command using SysLib.exec().
     Since SysLib.exec(String args[]) takes in a string array, the commands have to be outputted as a string array. The method is private
     to avoid memory leaks. Return null if the generateCmd is unsuccessful and the command otherwise. generateCommand also check if the command
     is valid before putting the command into the newCommand array.
     Pre-condition: Input of command line arguments have to be in a string array in order for generate command to process the command.
     The input of hte first element should be the index of PingPong or the class and the index of the last element should be the index of the
     delimiter such as ";" and "&".
     Post-condition: The output will be a return of a string array containing only one command such as "PingPong abc 4" without the delimiter.
      */
     private String[] generateCmd(String[] command, int firstElement, int lastElement){
         int theClassIndex = firstElement;
         int firstArgumentIndex = firstElement + 1;
         if(lastElement == 0){
             lastElement = lastElement + 1;
         }
         //We know that ";" and "&" is the delimiter of the end of command so we just need to move one index back to get the second argument.
         else if((command[lastElement - 1] == ";" || command[lastElement - 1] == "&") && lastElement < command.length){
             lastElement = lastElement - 1;
         }
 
         int secondArgumentIndex = lastElement - 1;
 
         //this newCommand array is what we will hold our command and return for our function
         //since our index may not always be 0,1, and 2 we need to fix the index accordingly
         String[] newCommand = new String[3];
 
         //The first two cases of the if statement shows that there is something wrong with our command and therefore we return null to show
         //that we failed to compile a command we return null
         if(lastElement < firstElement){
             return null;
         }
         else if(theClassIndex > secondArgumentIndex){
             return null;
         }
         else if(command.length == 1){
 
             if(!command[0].equals("exit")){
                 SysLib.cerr("Input not accepted. \n");
                 newCommand[0] = "x";
                 return newCommand;
             } else {
                 newCommand[0] = "exit";
                 SysLib.cout("System detected an exit.\n");
                 return newCommand;
             }
 
         }
         else {
             //The command.length has to be even
             //theClassIndex has to be PingPong
             //the secondArgumentIndex have to be an integer
             if((IsEven(command.length) != true) || (!command[theClassIndex].equals("PingPong")) || (IsInt(command[secondArgumentIndex]) == false)){
                 SysLib.cerr("Input not accepted. \n");
                 //if the command is wrong then we will maek the first command will be x
                 newCommand[0] = "x";
                 return newCommand;
             } else {
                 //if everything is right then we can insert the commands into the newCommand array
                 newCommand[0] = command[theClassIndex];
                 newCommand[1] = command[firstArgumentIndex];
                 newCommand[2] = command[secondArgumentIndex];
                 return newCommand;
             }
         }
 
     }
 
 
     /*boolean Isint(String string)
     ------------------------------------------------------------
     Description: Takes a string and check if it is an integer, returns true if the string is an integer and false otherwise.
     A string is taken as the input. This is a helper function used in generateCmd.
     NOTE: Most of the code for this simple function us taken from the link below so all credit goes to the user that posted this solution.
     https://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-numeric-in-java/10048788
      */
     private boolean IsInt(String string){
         try{
             Integer.parseInt(string);
             return true;
         } catch (NumberFormatException ex){
             return false;
         }
     }
 
     /*boolean IsEven(int num)
     ------------------------------------------------------------
     Description: Takes a number and check if it is even or not. If it is even then it returns true and false otherwise.
     Takes in a number as input. This is a helper function used in generateCmd.
    */
     private boolean IsEven(int num){
         //If even, anything divided by 2 should have no remainder
         if(num % 2 == 0){
             return true;
         } else {
             return false;
         }
     }
 }