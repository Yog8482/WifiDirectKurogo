# cd [指定のEclipseプロジェクトのパス]
# rubgy generatejar.rb
# 指定クラスの関係classファイルを検索してjarファイルに出力する
#
# 例
# 	cd /cygdrive/d/workspace/WifiDirectKurogo
# 	ruby generatejar.rb

require 'fileutils'

# 逐次変更する
jarout = "../WifiDirectManager.jar"
classNames = [
  "WifiDirectManager", 
  "MinimalMessage", "Message", "HelloContent", "IntentContent", 
  "BeginFileContent", "PartedFileContent", "EndFileContent",
  "WifiDirectService", "IWifiDirectService", "IWifiDirectCallback", 
  "Peer", "WifiDirectServiceListener", "TransferListener", "FileUtil", "ITransferCallback"]

  
puts "output jar file = #{jarout}"
puts "search class names = #{classNames.join(" ")}\n"

puts "hit files are:"
hitFiles = []
Dir.glob("**/*").each do |path|
  if path =~ /class$/
    next if path.include?("src/")
    rightIndex = path.include?("$") ? path.index("$")-1 : path.rindex(".")-1
    fileName = path[path.rindex("/")+1..rightIndex]
    # puts "fileName = #{fileName}"
    classNames.each do |className|
      if fileName == className
        puts path
        packPath = path.gsub("bin/classes/", "")
        newPath = "src/" + packPath
        FileUtils.cp(path, newPath)
        puts "\tis copied to #{newPath}"
        hitFiles.push packPath
      end
    end

  end
end

FileUtils.cd("src/")
com = "\njar cvf ../#{jarout} #{hitFiles.join(" ")}"
com.gsub!("$", "\\$") # 重要．$のままだとjarコマンドが正しく解釈しない
puts "exec command = #{com}"
exec(com)
FileUtils.cd("../")
