.PHONY: lint test

## Run all lint checks (add --fix to auto-correct)
lint:
	./lint.sh $(ARGS)

## Run all tests
test:
	./test.sh
