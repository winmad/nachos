#include "syscall.h"

char buf[1024];

int main()
{
	int out = creat("sb.txt");
	buf[0] = 's'; buf[1] = 'b'; buf[2] = '2'; buf[3] = 'b';
	write(out , buf , 4);
	close(out);
	int in = open("sb.txt");
	read(in , buf , 2);
	printf("%c%c\n" , buf[0] , buf[1]);
	close(in);
	//unlink("sb.txt");
	return 0;
}
