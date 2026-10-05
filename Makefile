COMPOSE = docker compose
COMPOSE_DEV = $(COMPOSE) -f compose.yaml -f compose.dev.yaml

all: up

up:
	@$(COMPOSE) up -d --build

down:
	@$(COMPOSE) down

logs:
	@$(COMPOSE) logs -f

ps:
	@$(COMPOSE) ps

clean:
	@$(COMPOSE) down --rmi local

fclean:
	@$(COMPOSE) down -v --rmi local

re: down up

dev:
	@$(COMPOSE_DEV) up -d --build db mailpit backend
	@cd front && npm run dev

.PHONY: all up down logs ps clean fclean re dev
