import { GraphiQL } from 'graphiql';
import 'graphiql/graphiql.css';

const fetcher = async graphQLParams => {
  const response = await fetch(
    'graphql',
    {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(graphQLParams),
      credentials: 'same-origin',
    },
  );
  return response.json();
};

const App = () => <GraphiQL fetcher={fetcher} />;

export default App;
