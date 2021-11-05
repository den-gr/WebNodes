/**
 * Manage all webRTC connections
 */
class WebrtcManager{

	// Array with all connected peers
	#peers = new Map();
	
	constructor(webSocket){
		this.webSocket = webSocket;
	}

	setStrategy(channelStrategy){
		this.channelStrategy = channelStrategy;
	}

	//Start a new connection with an peer
	#addConnection(channel_id, peer_id){
		this.#peers.set(channel_id, new ConnectedPeer(channel_id, peer_id, this.localNodeId, this.channelStrategy, this.webSocket)); 
		this.#peers.get(channel_id).createNewChannel();
	}

	// send a msg to all peers
	sendMsgToConnectedNodes(msg){
		let channelsIterator = this.#peers.values();

        let entry = channelsIterator.next();
        while (!entry.done) {
			console.log("State: " , entry.value.getConnection().iceConnectionState);
			if(entry.value.getConnection().iceConnectionState === 'disconnected' 
				|| entry.value.channel.readyState !== 'open'){
				//if a peer is disconnected, remove it from array
				// probably is needed more stable way to do it 
				console.log("remove a connection");
				this.#peers.delete(entry.key);
			}else{
				//send msg
				entry.value.channel.send(msg);
			}
            entry = channelsIterator.next();
        }
	}

	// if a new msg is received
	async elaborateMsg(json){
		let channel_id = json.channel_id;
		let peer = this.#peers.has(channel_id) ? this.#peers.get(channel_id) : null;
		if(this.localNodeId != json.receiver_id){
			console.error("This node have not to receive this message");
		}
		if(json.desc){
			if (json.desc.type === 'offer') { // receive an handshake offer from another peer
				this.#peers.set(channel_id, new ConnectedPeer(channel_id, 
															  json.sender_id, 
															  this.localNodeId, 
															  this.channelStrategy, 
															  this.webSocket)); 
				peer = this.#peers.get(channel_id);
				await peer.getConnection().setRemoteDescription(json.desc);
				await peer.getConnection().setLocalDescription(await peer.getConnection().createAnswer());
				let obj = {type: "signaling",
						   receiver_id: peer.getPeerId(),
						   desc: peer.getConnection().localDescription, 
						   channel_id: channel_id}
				this.webSocket.send(JSON.stringify(obj));
			} else if (json.desc.type === 'answer') { // receive an handshake answer from another peer
				await peer.getConnection().setRemoteDescription(json.desc);
			} else {
				console.log('Unsupported SDP type.');
			}

		}else if(json.ice){ // receive an ice candidate from another peer
			console.log("received candidate: ");
			await peer.getConnection().addIceCandidate(JSON.parse(json.ice));
		}else{
			console.log("The received msg is not recognized: ", json);
		}
	}

	//If there is a peer that must be connected so start a new connection, if it can't be done right now, so set the flag #connectionAvailable
	connectionAvailable(to_be_connected){
		for (var i = 0; i < to_be_connected.length; i++) {
			this.#addConnection(to_be_connected[i].channel_id, 
								to_be_connected[i].node_id);
		}
	}
	setLocalNodeId(localNodeId){
		this.localNodeId = localNodeId;
	}
}

/**
 * A class that incapsulate a single connection with an peer
 */
class ConnectedPeer{
	//static STUN_SERVER_URL = 'stun:stun.l.google.com:19302';
	
	/**
	 * @param {number} channel_id unique id of channel between nodes
	 * @param {number} peer_id id of remote node
	 * @param {number} localNodeId id of local node 
	 * @param {ChannelStrategy} strategy for handling the received messeges 
	 * @param {WebSocket} serverWebSocket needed for sending messeges to server
	 */
	constructor(channel_id, peer_id, localNodeId, strategy, serverWebSocket){
		this.serverWebSocket = serverWebSocket;
		this.strategy = strategy;

		this.channel_id = channel_id;
		this.peer_id = peer_id;
		this.localNodeId = localNodeId;
		
		//let configuration = {iceServers: [{urls: ConnectedPeer.STUN_SERVER_URL}]};
		this.peerConnection =  new RTCPeerConnection(/*configuration*/);
		this.peerConnection.ondatachannel = (event) =>  this.#setUpChannel(event.channel);
		this.peerConnection.oniceconnectionstatechange = (e) => {
			if(this.peerConnection.iceConnectionState == 'disconnected'){
				this.strategy.onDisconnected(this.channel, this.#getChannelId());
			}
		};
	}
	
	//Create a new channel
	createNewChannel(){
		this.#setUpChannel(this.peerConnection.createDataChannel("Channel" + this.#getChannelId()));

		// Send any ice candidates to the other peer.
		this.peerConnection.onicecandidate = e => {
			if(e.candidate != null) {
				let json = {type: "signaling",
							receiver_id: this.peer_id,
							ice : JSON.stringify(e.candidate.toJSON()), 
							channel_id: this.#getChannelId()}
				this.serverWebSocket.send(JSON.stringify(json))
			}
				
		}

		// Let the "negotiationneeded" event trigger offer generation.
		this.peerConnection.onnegotiationneeded = async () => {
			try {
				await this.peerConnection.setLocalDescription(await this.peerConnection.createOffer());
				// Send the offer to the other peer.
				let obj = {type: "signaling", 
							sender_id: this.localNodeId,
							receiver_id: this.peer_id, 
							desc: this.peerConnection.localDescription, 
							channel_id: this.#getChannelId()};
				this.serverWebSocket.send(JSON.stringify(obj));
			} catch (err) {
				console.error(err);
			}
		};
	}

	getConnection(){
		return this.peerConnection;
	}

	//return id of node that must be connected with this connection
	getPeerId(){
		return this.peer_id;
	}

	#setUpChannel(newChannel) {
		this.channel = newChannel;
		
		this.channel.onopen = () => this.strategy.onOpen(this.channel, this.#getChannelId());
		this.channel.onmessage = (m) => this.strategy.onMessage(m, this.channel , this.#getChannelId());
		this.channel.onclose = () => this.strategy.onClose(this.channel , this.#getChannelId()); 

		this.channel.onerror = function(event) {
			console.error(
			  'Network Error. The error "' + event.error + '" occurred while handling player control network messages. ' + "\n" +
			  event.filename +  "\n" +
			  event.lineno +  "\n" +
			  event.colno +  "\n" + "Event : "+ event
			);
		  }
	}

	#getChannelId(){
		return this.channel_id;
	}
}
