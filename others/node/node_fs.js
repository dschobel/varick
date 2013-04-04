var net = require('net');
var fs = require('fs');

var server = net.createServer(function (socket) {
    fs.readFile('./data.txt', function (err, data) {
        console.log("read file fired")
        if (err) throw err;
        socket.write(data);
        socket.end();
    });
    console.log("connect fx returned")
});

server.listen(1337, '127.0.0.1');
