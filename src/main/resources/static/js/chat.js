const state = {
	conversationId: null,
	isSending: false,
	selectedFiles: [],
	isUploadingFiles: false
};

const messagesEl = document.querySelector("#messages");
const emptyStateEl = document.querySelector("#emptyState");
const conversationListEl = document.querySelector("#conversationList");
const formEl = document.querySelector("#chatForm");
const inputEl = document.querySelector("#messageInput");
const sendButtonEl = document.querySelector("#sendButton");
const toastEl = document.querySelector("#toast");
const refreshButtonEl = document.querySelector("#refreshConversations");
const modelStatusEl = document.querySelector("#modelStatus");
const fileFormEl = document.querySelector("#fileForm");
const fileInputEl = document.querySelector("#fileInput");
const submitFilesEl = document.querySelector("#submitFiles");
const fileStatusListEl = document.querySelector("#fileStatusList");

formEl.addEventListener("submit", sendMessage);
refreshButtonEl.addEventListener("click", loadConversations);
fileInputEl.addEventListener("change", selectFiles);
fileFormEl.addEventListener("submit", submitFiles);
inputEl.addEventListener("keydown", (event) => {
	if ((event.metaKey || event.ctrlKey) && event.key === "Enter") {
		formEl.requestSubmit();
	}
});

loadConversations();

async function loadConversations() {
	try {
		const conversations = await getJson("/api/conversations");
		renderConversations(conversations);
		if (!state.conversationId && conversations.length > 0) {
			await selectConversation(conversations[0].id);
		}
	}
	catch (error) {
		showToast(error.message);
	}
}

async function selectConversation(conversationId) {
	state.conversationId = conversationId;
	highlightActiveConversation();
	try {
		const messages = await getJson(`/api/conversations/${conversationId}/messages`);
		renderMessages(messages);
	}
	catch (error) {
		showToast(error.message);
	}
}

async function sendMessage(event) {
	event.preventDefault();
	if (state.isSending) {
		return;
	}

	const message = inputEl.value.trim();
	if (!message) {
		inputEl.focus();
		return;
	}

	state.isSending = true;
	sendButtonEl.disabled = true;
	inputEl.value = "";
	showToast("");
	appendMessage({
		role: "user",
		content: message,
		createdAt: new Date().toISOString()
	});
	const thinkingEl = appendMessage({
		role: "assistant",
		content: "Thinking...",
		modelName: "llama",
		createdAt: new Date().toISOString()
	});

	try {
		const response = await postJson("/api/chat", {
			conversationId: state.conversationId,
			message
		});
		thinkingEl.remove();
		state.conversationId = response.conversation.id;
		appendMessage(response.assistantMessage);
		renderConversations(response.conversations);
		highlightActiveConversation();
		modelStatusEl.textContent = response.assistantMessage.modelName || "Ollama";
	}
	catch (error) {
		thinkingEl.remove();
		showToast(error.message);
	}
	finally {
		state.isSending = false;
		sendButtonEl.disabled = false;
		inputEl.focus();
		updateEmptyState();
	}
}

function selectFiles() {
	state.selectedFiles = Array.from(fileInputEl.files || []).map((file) => ({
		file,
		fileName: file.name,
		status: "pending",
		message: "Ready to submit",
		sizeBytes: file.size,
		chunksStored: 0
	}));
	renderFileStatuses();
}

async function submitFiles(event) {
	event.preventDefault();
	if (state.isUploadingFiles || state.selectedFiles.length === 0) {
		return;
	}

	state.isUploadingFiles = true;
	submitFilesEl.disabled = true;
	state.selectedFiles = state.selectedFiles.map((item) => ({
		...item,
		status: "uploading",
		message: "Uploading and indexing"
	}));
	renderFileStatuses();
	showToast("");

	const formData = new FormData();
	for (const item of state.selectedFiles.filter((selectedFile) => selectedFile.file)) {
		formData.append("files", item.file);
	}
	if (!formData.has("files")) {
		state.isUploadingFiles = false;
		showToast("Select at least one text file first.");
		updateFileSubmitState();
		return;
	}

	try {
		const response = await fetch("/api/rag/files", {
			method: "POST",
			headers: { "Accept": "application/json" },
			body: formData
		});
		const payload = await readJsonResponse(response);
		state.selectedFiles = payload.files || [];
		renderFileStatuses();
		const storedCount = state.selectedFiles.filter((item) => item.status === "stored").length;
		showToast(storedCount > 0 ? `${storedCount} file${storedCount === 1 ? "" : "s"} stored for RAG.` : "No files were stored.");
	}
	catch (error) {
		state.selectedFiles = state.selectedFiles.map((item) => ({
			...item,
			status: "failed",
			message: error.message
		}));
		renderFileStatuses();
		showToast(error.message);
	}
	finally {
		state.isUploadingFiles = false;
		fileInputEl.value = "";
		updateFileSubmitState();
	}
}

async function getJson(url) {
	const response = await fetch(url, {
		headers: { "Accept": "application/json" }
	});
	return readJsonResponse(response);
}

async function postJson(url, body) {
	const response = await fetch(url, {
		method: "POST",
		headers: {
			"Accept": "application/json",
			"Content-Type": "application/json"
		},
		body: JSON.stringify(body)
	});
	return readJsonResponse(response);
}

async function readJsonResponse(response) {
	const payload = await response.json().catch(() => ({}));
	if (!response.ok) {
		throw new Error(payload.message || "Request failed");
	}
	return payload;
}

function renderConversations(conversations) {
	conversationListEl.replaceChildren();
	if (conversations.length === 0) {
		const empty = document.createElement("div");
		empty.className = "conversation-meta";
		empty.textContent = "No conversations yet";
		conversationListEl.append(empty);
		return;
	}

	for (const conversation of conversations) {
		const button = document.createElement("button");
		button.type = "button";
		button.className = "conversation-button";
		button.dataset.conversationId = conversation.id;
		button.addEventListener("click", () => selectConversation(conversation.id));

		const title = document.createElement("div");
		title.className = "conversation-title";
		title.textContent = conversation.title || formatDate(conversation.date);

		const meta = document.createElement("div");
		meta.className = "conversation-meta";
		meta.textContent = `${formatDate(conversation.date)} - ${conversation.messageCount} messages`;

		button.append(title, meta);
		conversationListEl.append(button);
	}
	highlightActiveConversation();
}

function renderMessages(messages) {
	messagesEl.replaceChildren();
	for (const message of messages) {
		appendMessage(message);
	}
	updateEmptyState();
	scrollToBottom();
}

function renderFileStatuses() {
	fileStatusListEl.replaceChildren();
	if (state.selectedFiles.length === 0) {
		const empty = document.createElement("div");
		empty.className = "file-empty";
		empty.textContent = "No files selected";
		fileStatusListEl.append(empty);
		updateFileSubmitState();
		return;
	}

	for (const item of state.selectedFiles) {
		const row = document.createElement("article");
		row.className = `file-status ${item.status}`;

		const details = document.createElement("div");
		details.className = "file-details";

		const name = document.createElement("div");
		name.className = "file-name";
		name.textContent = item.fileName;

		const meta = document.createElement("div");
		meta.className = "file-meta";
		meta.textContent = fileMetaText(item);

		details.append(name, meta);

		const badge = document.createElement("span");
		badge.className = "file-badge";
		badge.textContent = item.status;

		row.append(details, badge);
		fileStatusListEl.append(row);
	}
	updateFileSubmitState();
}

function appendMessage(message) {
	const wrapper = document.createElement("article");
	wrapper.className = `message ${message.role === "user" ? "user" : "assistant"}`;

	const meta = document.createElement("div");
	meta.className = "message-meta";
	meta.textContent = message.role === "user"
		? `You - ${formatTime(message.createdAt)}`
		: `${message.modelName || "Llama"} - ${formatTime(message.createdAt)}`;

	const bubble = document.createElement("div");
	bubble.className = "message-bubble";
	bubble.textContent = message.content;

	wrapper.append(meta, bubble);
	messagesEl.append(wrapper);
	updateEmptyState();
	scrollToBottom();
	return wrapper;
}

function highlightActiveConversation() {
	for (const button of conversationListEl.querySelectorAll(".conversation-button")) {
		button.classList.toggle("active", Number(button.dataset.conversationId) === state.conversationId);
	}
}

function updateEmptyState() {
	emptyStateEl.style.display = messagesEl.children.length === 0 ? "grid" : "none";
}

function scrollToBottom() {
	messagesEl.scrollTop = messagesEl.scrollHeight;
}

function showToast(message) {
	toastEl.textContent = message;
}

function updateFileSubmitState() {
	submitFilesEl.disabled = state.isUploadingFiles || state.selectedFiles.length === 0;
}

function fileMetaText(item) {
	if (item.status === "stored") {
		return `${formatBytes(item.sizeBytes)} - ${item.chunksStored} chunks`;
	}
	return `${formatBytes(item.sizeBytes)} - ${item.message || item.status}`;
}

function formatBytes(bytes) {
	if (!bytes) {
		return "0 B";
	}
	const units = ["B", "KB", "MB", "GB"];
	let size = bytes;
	let unitIndex = 0;
	while (size >= 1024 && unitIndex < units.length - 1) {
		size /= 1024;
		unitIndex += 1;
	}
	return `${size.toFixed(size >= 10 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
}

function formatDate(value) {
	if (!value) {
		return "Today";
	}
	return new Intl.DateTimeFormat(undefined, {
		month: "short",
		day: "numeric",
		year: "numeric"
	}).format(new Date(`${value}T00:00:00`));
}

function formatTime(value) {
	if (!value) {
		return "now";
	}
	return new Intl.DateTimeFormat(undefined, {
		hour: "2-digit",
		minute: "2-digit"
	}).format(new Date(value));
}

renderFileStatuses();
