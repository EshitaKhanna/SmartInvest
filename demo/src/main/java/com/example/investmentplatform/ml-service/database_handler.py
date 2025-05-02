import datetime
import psycopg2

class DatabaseHandler:
    def __init__(self, dbname, user, password, host="localhost"):
        self.conn = psycopg2.connect(
            dbname=dbname,
            user=user,
            password=password,
            host=host
        )
        self.cursor = self.conn.cursor()

    def save_sentiment_data(self, stock_symbol, title, content, sentiment_score):
        query = """
            INSERT INTO sentiment_history (stock_symbol, timestamp, title, content, sentiment_score)
            VALUES (%s, %s, %s, %s, %s)
        """
        try:
            self.cursor.execute(query, (stock_symbol, datetime.datetime.now(), title, content, sentiment_score))
            self.conn.commit()
        except Exception as e:
            print(f"Error inserting data: {e}")
            self.conn.rollback()  # Rollback the transaction on error

    def close(self):
        self.cursor.close()
        self.conn.close()