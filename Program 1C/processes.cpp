/*Ruiqi Huang
1/17/2024
CSS 430
Program 1C
-------------------------------------------------------------------------------------------------------
Description: Our program is created to mirror the command of ps -A | grep argv[1] | wc -l  from linux where argv[1]
is the process name. This command takes a processes name and finds it from the list of all processes running in the system
and prints out the number processes with that name. ps -A lists all the processes running in the system, grep gets the 
name of the processes and wc -l prints out the number of processes with that name. 
Our program utilizes system calls such as dup2, wait, pipe, fork, and exec to imitate how the processes would run in linux.
It shows how linux uses pipes to initate commands as well. 

*/

#include <iostream> //for cerr, cout
#include <stdlib.h> //for exit
#include <stdio.h> //for perror
#include <unistd.h> //for fork, pipe
#include <sys/wait.h> //for wait
#include <fcntl.h>

using namespace std;

/*
*/
int main(int argc, char* argv[]) {
    //pipe fd index RD=0, WR=1
    enum {RD, WR}; 

    //These are the file descriptors for our pipes, (1) pipe 1 (2) pipe 2
    int fileDescriptor1[2]; //(1)
    int fileDescriptor2[2]; //(2)

    //The pid of each child, since we have 3 we will have 3 different pid
    pid_t pidChildOne, pidChildTwo, pidChildThree;

    //the beginning checks for all possible failures
    //create pipe 1
    if(pipe(fileDescriptor1) < 0){
        perror("pipe error");
        exit(EXIT_FAILURE);
    }
    //create pipe 2
    else if(pipe(fileDescriptor2) < 0){
        perror("pipe error");
        exit(EXIT_FAILURE);
    } 
    //fork to child one
    else if((pidChildOne = fork()) < 0){
        perror("fork1 error");
        exit(EXIT_FAILURE);
    } 
    //fork to child two
    else if(pidChildOne == 0){
        if((pidChildTwo = fork()) < 0){
            perror("fork2 error");
            exit(EXIT_FAILURE);
        } 
        else if(pidChildTwo == 0){
            //fork to child three
            if((pidChildThree = fork()) < 0){
                perror("fork3 error");
                exit(EXIT_FAILURE);
            } 
            else if(pidChildThree == 0){
                //------------------ CHILD THREE (GREAT-GRAND-CHILD) ---------------
                //closing all unused pipes
                close(fileDescriptor2[WR]);
                close(fileDescriptor2[RD]);
                close(fileDescriptor1[RD]); 

                //child three uses pipe 1 to write 
                dup2(fileDescriptor1[WR], WR); 
                wait(NULL);
                execlp("/bin/ps", "ps", "-A", NULL); 
                //cout << "sucessfully executed ps - A" << endl; //For testing purposes only
            } 
            else {
                //--------------------- CHILD TWO (GRAND-CHILD) ---------------------
                //close all unused pipes
                close(fileDescriptor1[WR]); 
                close(fileDescriptor2[RD]); 
                
                //child two uses pipe 1 to read
                dup2(fileDescriptor1[RD],RD); 
                wait(NULL);
                close(fileDescriptor1[RD]); 
                
                //child two uses pipe 2 to write
                dup2(fileDescriptor2[WR],WR); 
                execlp("/bin/grep", "grep", argv[1], NULL); 
                //cout << "successfully executed grep argsv[1]" << endl; //for testing purposes only
                
            }
        } 
        else {
            //---------------------- CHILD ONE (CHILD) ----------------------
            //closing all unused pipes
            close(fileDescriptor1[WR]); 
            close(fileDescriptor1[RD]); 
            close(fileDescriptor2[WR]); 

            //child one uses pipe 2 to read
            dup2(fileDescriptor2[RD],RD); 
            wait(NULL);
            execlp("/bin/wc", "wc", "-l", NULL); 
            //cout << "successfully executed wc -l" << endl; //for testing purposes only
        }
    } 
    else {
        //---------------------------------- PARENT --------------------------
        //closing all unused pipes
        close(fileDescriptor1[RD]);
        close(fileDescriptor1[WR]);
        close(fileDescriptor2[RD]);
        close(fileDescriptor2[WR]);

        //calling wait to wait for childrens to finish
        wait(NULL); 
        cout << "done!" << endl;
    }
    exit(EXIT_SUCCESS);
    return 0; 
}
