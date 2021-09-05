const serverURL = 'ws://localhost:8081';
//const serverURL = 'ws://1104-5-170-128-165.ngrok.io'
const MAX_NODES = 10;
let pages = [];
let webSocket;


window.onbeforeunload = function(){
    pages.forEach(item => item.close());
};


window.onload = function(){
	setUpSocket();
}


function setUpSocket(){
	webSocket = new WebSocket(serverURL);

	webSocket.onopen = () => {
        console.log('open');
        //ask for generator setup
        webSocket.send(JSON.stringify({"type": "generator_setup_demand"}));
}
	webSocket.onclose = () => console.log('close');
	webSocket.onerror = () => console.log('error', error.message);

	webSocket.onmessage = async function(msg) {
        console.log(msg.data);
        try{	
            let json = JSON.parse(msg.data);
            if(json.type == "generator_setup"){
                pages.forEach(item => item.close());
                if(json.node_quantity > MAX_NODES ){
                    console.log("Can not create more nodes that max limit: ", MAX_NODES);
                    json.node_quantity = MAX_NODES;
                }
                for(let i = 0; i < json.node_quantity; i++){
                    pages.push(window.open('http://localhost:8081/static/tnode.html', "_blank"));
                    //pages.push(window.open('http://1104-5-170-128-165.ngrok.io/static/tnode.html', "_blank"));
                     await sleep(200);
                }
                document.getElementById('number_of_nodes').textContent = pages.length;
            }else if(json.type == "close_all_nodes"){
                pages.forEach(item => item.close());
            }
            
        }catch(err){
            console.error(err);
        }
	};
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}