#download index
wget http://www.khronos.org/opencl/sdk/1.0/docs/man/xhtml/Opencl_tofc.html;
#find links to cl* function doc
grep -E .+\<a\ href=\"cl[A-Z][^\"]+\"[^\>]+\>cl[A-Z][a-Z]+\</a\>.+ ./Opencl_tofc.html > links;
#add doc root to properties file
echo nativetaglet.baseUrl=http://www.khronos.org/opencl/sdk/1.0/docs/man/xhtml/ > native-taglet.properties;
#add all links as properties to file and cleanup
sed -r 's/\s+<li><a href="([a-Z.]+)"[^>]+>([a-Z]+)<\/a><\/li>/\2=\1/' links >> native-taglet.properties;
rm ./Opencl_tofc.html ./links
