/**
 * Manage all webRTC connections
 */
class WebrtcManager{
	// if true, after finishing handshake, create a new connection
	#connectionAvailable = false;
	// Array with all connected peers
	#peers = []

	#connection_id = 0;

	//Start a new connection with an peer
	addConnection(){
		this.#peers.push(new ConnectedPeer(this, this.#connection_id++)); 
		this.#getLastPeer().newChannel();
	}

	// send a msg to all peers
	sendToAll(msg){
		for(let i = 0; i < this.#peers.length; i++){
			console.log("State: " ,this.#peers[i].peerConnection.iceConnectionState);
			if(this.#peers[i].peerConnection.iceConnectionState === 'disconnected' 
				|| this.#peers[i].channel.readyState !== 'open'){
				//if a peer is disconnected, remove it from array
				// probably is needed more stable way to do it 
				console.log("remove a connection");
				this.#peers.splice(i,1);
			}else{
				//send msg
				this.#peers[i].channel.send(msg);
			}
		}
	}

	// send(channelId, msg){
	// 	for(let i = 0; i < this.#peers.length; i++){
	// 		if(this.#peers[i].getChannelId() === channelId){
	// 			this.#peers[i].channel.send(msg);
	// 			return;
	// 		}
	// 	}	
	// 	console.err("Channel with id " + channelId + "is not found");
	// }



	// if a new msg is received
	async elaborateMsg(json){
		if(json.desc){
			if (json.desc.type === 'offer') { // receive an handshake offer from another peer
				this.#peers.push(new ConnectedPeer(this, this.#connection_id++)); 
				await this.#getLastPeer().peerConnection.setRemoteDescription(json.desc);
				await this.#getLastPeer().peerConnection.setLocalDescription(await this.#getLastPeer().peerConnection.createAnswer());
				webSocket.send(JSON.stringify({desc: this.#getLastPeer().peerConnection.localDescription}));
			} else if (json.desc.type === 'answer') { // receive an handshake answer from another peer
				await this.#getLastPeer().peerConnection.setRemoteDescription(json.desc);
			} else {
				console.log('Unsupported SDP type.');
			}

		}else if(json.candidate){ // receive an handshake candidate from another peer
			console.log("received candidate: ");
			await this.#getLastPeer().peerConnection.addIceCandidate(json);
		}else{
			console.log("The received msg is not recognized: ", json);
		}
	}

	//If there is a peer that must be connected so start a new connection, if it can't be done right now, so set the flag #connectionAvailable
	connectionAvailable(){
		console.log("last peer connection state",  this.#getLastPeer() ? this.#getLastPeer().peerConnection.iceConnectionState : null);
		if(!this.#getLastPeer()){
			this.addConnection()

		}else if(this.#getLastPeer().peerConnection.iceConnectionState === "connected"){
			this.addConnection()
		}else{
			this.#connectionAvailable = true;
		}
	}

	// return a last added peer
	#getLastPeer(){
		return this.#peers[this.#peers.length -1];
	}

	//An peer use this method for notify WebrtcManager about completion of the connection precess
	notifyConnectionComplete(){
		if(this.#connectionAvailable){
			console.log("Another connections are available so go to add them");
			this.addConnection();
			this.#connectionAvailable = false;
		}
	}
}

/**
 * A class that incapsulate a single connection with an peer
 */
class ConnectedPeer{
	static STUN_SERVER_URL = 'stun:stun.l.google.com:19302';
	constructor(observer, id){
		this.id = id;
		this.observer = observer;
		
		let configuration = {iceServers: [{url: ConnectedPeer.STUN_SERVER_URL}]};
		this.peerConnection =  new RTCPeerConnection(configuration);
		this.peerConnection.ondatachannel = (event) =>  this.#setUpChannel(event.channel);
		this.peerConnection.oniceconnectionstatechange = (e) => {
			if(this.peerConnection.iceConnectionState == 'disconnected'){
				console.log("disconnected");
				node.removeConnectedNode(this.getChannelId());
			}
		};
	}
	
	//Create a new channel
	newChannel(){
		this.#setUpChannel(this.peerConnection.createDataChannel("Channel", {id: this.getChannelId()}));

		// Send any ice candidates to the other peer.
		this.peerConnection.onicecandidate = e => {if(e.candidate != null) webSocket.send(JSON.stringify(e.candidate))}

		// Let the "negotiationneeded" event trigger offer generation.
		this.peerConnection.onnegotiationneeded = async () => {
			try {
				await this.peerConnection.setLocalDescription(await this.peerConnection.createOffer());
				// Send the offer to the other peer.
				let obj = {desc: this.peerConnection.localDescription};
				webSocket.send(JSON.stringify(obj));
			} catch (err) {
				console.error(err);
			}
		};
	}

	#setUpChannel(newChannel) {
		this.channel = newChannel;
		this.channel.onopen = () => {
			console.log("Open a new P2P connection");//handleChannelOpen;
			this.observer.notifyConnectionComplete();
			this.channel.send(JSON.stringify({"type": "hello", "id": node.id}))
		}
		this.channel.onmessage = (m) => {
			console.log("A messge from p2p channel " + this.getChannelId() + ":", JSON.parse(m.data));
			let json = JSON.parse(m.data);
			if(json.type == "hello"){
				console.log("Hello type");
				node.addConnectedNode(this.getChannelId(), json.id);
			}else if(json.type == "movementTo"){
				if(node.isOutOfRadius(json.x, json.y)){
					console.log("out of radius");
					node.removeConnectedNode(this.getChannelId());
					this.channel.close();
				}
			}
		}
		this.channel.onclose = () => {
			console.log("Close a P2P connection"); //handleChannelClose;
			node.removeConnectedNode(this.getChannelId());
		} 
	}

	getChannelId(){
		return this.id;
	}
}

