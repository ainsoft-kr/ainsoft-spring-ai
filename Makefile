PIPER_MODEL_DIR=./data/model
PIPER_MODEL_NAME=en_US-lessac-medium
PIPER_BASE_URL=https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/medium

.PHONY: piper-model
piper-model:
	mkdir -p $(PIPER_MODEL_DIR)
	curl -L -o $(PIPER_MODEL_DIR)/$(PIPER_MODEL_NAME).onnx $(PIPER_BASE_URL)/$(PIPER_MODEL_NAME).onnx
	curl -L -o $(PIPER_MODEL_DIR)/$(PIPER_MODEL_NAME).onnx.json $(PIPER_BASE_URL)/$(PIPER_MODEL_NAME).onnx.json
