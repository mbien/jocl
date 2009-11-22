#download index
root=http://www.khronos.org/opencl/sdk/1.0/docs/man/xhtml
wget ${root}/Opencl_tofc.html;
#find links to cl* function doc
grep -E .+\<a\ href=\"cl[A-Z][^\"]+\"[^\>]+\>cl[A-Z][a-Z0-9]+\</a\>.+ ./Opencl_tofc.html > links;
#add doc root to properties file
echo "#Generated, do not edit, edit createTagletProps.sh instead.
#This file is used in NativeTaglet and maps the generated method names
#to the function specific OpenCL documentation man pages.
nativetaglet.baseUrl=${root}" > native-taglet.properties;
#add all links as properties to file and cleanup
sed -r 's/\s+<li><a href="([a-Z0-9.]+)"[^>]+>([a-Z0-9]+)<\/a><\/li>/\2=\1/' links >> native-taglet.properties;
rm ./Opencl_tofc.html ./links
