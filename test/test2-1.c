#include "syscall.h"
#include "stdio.h"

int main()
{
	int ret1 = -2, ret2 = -2, ret3 = -2, ret4 = -2, ret5 = -2, ret6 = -2;
	char txt[1024];

	strcpy(txt , "zhongguogongchandangshizhongdaibiaozhongguoxianjinshengchanlidefazhanyaoqiu,daibiaozhongguoxianjinwenhuadeqianjinfangxiang!");
	ret1 = creat("zhongguogongchandangwansui.txt");
	if(ret1 > 1)
		printf("Create sucessfully!");
	ret2 = write(ret1, (void *)txt, strlen(txt));
	printf("%d characters written!\n", ret2);
	ret3 = read(ret1, (void *)txt, 1000);
	printf("%d characters read!\n", ret3);
	ret4 = open("zhongguogongchandangwansui.txt"); 
	if(ret4 > 1)	
		printf("Open successfully!\n");
	ret5 = close(ret1);
	if(ret5 == 0)
		printf("Close successfully!\n");
	ret6 = unlink("zhongguogongchandangwansui.txt");
	if(ret6 == 0)
		printf("Unlink successfully!\n");
	ret4 = open("zhongguogongchandangwansui.txt"); 
	if(ret4 <= 1)	
		printf("Open failed gracefuly!\n");
	ret2 = write(ret1, (void *)txt, strlen(txt));
	if(ret2 <= 0)
		printf("Write failed gracefully\n");
	ret3 = read(ret1, (void *)txt, 1000);
	if(ret3 <= 0)
		printf("Read failed gracefully\n");
	ret4 = open("zhongguogongchandangwansui.txt"); 
	return 0;
}
