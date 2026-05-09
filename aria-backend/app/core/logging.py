import logging
import sys


def setup_logging() -> None:
    formatter = logging.Formatter(
        fmt="[ARIA] %(asctime)s %(levelname)s %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )

    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(formatter)

    logger = logging.getLogger("aria")
    logger.setLevel(logging.DEBUG)
    logger.addHandler(handler)

    uvicorn_logger = logging.getLogger("uvicorn")
    uvicorn_logger.handlers = [handler]
