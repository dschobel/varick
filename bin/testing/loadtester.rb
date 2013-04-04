require 'socket'
require './lib/thread-pool'



if ARGV.length < 3
    puts("usage is: loadtester.rb $requests $concurrency $port ($sleep)") 
    exit! 
end


requests = ARGV[0].to_i
concurrency = ARGV[1].to_i
port = ARGV[2].to_i
sleep_time = ARGV[3] || "0"


puts "making #{requests} requests with a concurrency level of #{concurrency} to #{port} (sleeping #{sleep_time} before reading)"
pool = Pool.new(concurrency)

start_time = Time.now
requests.times do
    pool.schedule do
        s = TCPSocket.open('localhost',port)
        s.puts("hi!")
        sleep(sleep_time.to_i)
        res = s.recv(128)
        #puts("got #{res}")
        s.close
    end
end
pool.shutdown #blocks until all tasks have finished
end_time = Time.now
elapsed_time = end_time - start_time
puts "all done in #{elapsed_time} seconds"

puts "that's #{(requests / elapsed_time).round(2)} clients per second"
