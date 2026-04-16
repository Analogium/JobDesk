.PHONY: setup lint test

## First-time setup: install git hooks
setup:
	git config core.hooksPath .githooks
	@echo "✔ Git hooks installed (.githooks/pre-push)"

## Run all lint checks (add --fix to auto-correct)
lint:
	./lint.sh $(ARGS)

## Run all tests
test:
	./test.sh
