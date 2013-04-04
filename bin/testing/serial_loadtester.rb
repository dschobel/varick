require 'socket'



if ARGV.length != 2
    puts("usage is: loadtester.rb $requests $port") 
    exit! 
end


requests = ARGV[0].to_i
port = ARGV[1].to_i




puts "making #{requests} requests to #{port}"

start_time = Time.now
requests.times do
        s = TCPSocket.open('localhost',port)
        s.puts("hi!")
        res = s.recv(128)
        #puts("got #{res}")
        s.close
end
end_time = Time.now
elapsed_time = end_time - start_time
puts "all done in #{elapsed_time} seconds"

puts "that's #{(requests / elapsed_time).round(2)} clients per second"
