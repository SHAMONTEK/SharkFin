import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/transaction.dart';
import '../theme_provider.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final themeProvider = Provider.of<ThemeProvider>(context);
    final transactions = _getMockTransactions(); // Mock data

    return Scaffold(
      appBar: AppBar(
        title: const Text('Fin-Pal'),
        actions: [
          IconButton(
            icon: Icon(themeProvider.themeMode == ThemeMode.dark
                ? Icons.light_mode
                : Icons.dark_mode),
            onPressed: () => themeProvider.toggleTheme(),
            tooltip: 'Toggle Theme',
          ),
          IconButton(
            icon: const Icon(Icons.auto_mode),
            onPressed: () => themeProvider.setSystemTheme(),
            tooltip: 'Set System Theme',
          ),
        ],
      ),
      body: Container(
        decoration: BoxDecoration(
          image: DecorationImage(
            image: const AssetImage("assets/images/noise.png"),
            fit: BoxFit.cover,
            colorFilter: ColorFilter.mode(
              Theme.of(context).colorScheme.background.withOpacity(0.05),
              BlendMode.dstATop,
            ),
          ),
        ),
        child: Column(
          children: [
            _buildFinancialSummaryCard(context),
            const SizedBox(height: 20),
            _buildRecentTransactions(context, transactions),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {},
        tooltip: 'Add Transaction',
        child: const Icon(Icons.add),
        elevation: 8,
        shape: const CircleBorder(),
      ),
    );
  }

  Widget _buildFinancialSummaryCard(BuildContext context) {
    return Container(
      margin: const EdgeInsets.all(16),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 10,
            offset: const Offset(0, 5),
          ),
        ],
      ),
      child: const Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          _FinancialSummaryItem(title: 'Income', amount: '\$5,000'),
          _FinancialSummaryItem(title: 'Expenses', amount: '\$2,500'),
          _FinancialSummaryItem(title: 'Balance', amount: '\$2,500'),
        ],
      ),
    );
  }

  Widget _buildRecentTransactions(
      BuildContext context, List<Transaction> transactions) {
    return Expanded(
      child: ListView.builder(
        itemCount: transactions.length,
        itemBuilder: (context, index) {
          final transaction = transactions[index];
          return Card(
            margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            elevation: 4,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
            child: ListTile(
              leading: CircleAvatar(
                backgroundColor: transaction.type == TransactionType.income
                    ? Colors.green.withOpacity(0.2)
                    : Colors.red.withOpacity(0.2),
                child: Icon(
                  transaction.icon,
                  color: transaction.type == TransactionType.income
                      ? Colors.green
                      : Colors.red,
                ),
              ),
              title: Text(transaction.title),
              subtitle: Text(transaction.date.toString()),
              trailing: Text(
                '${transaction.type == TransactionType.income ? '+' : '-'}\$${transaction.amount.toStringAsFixed(2)}',
                style: TextStyle(
                  color: transaction.type == TransactionType.income
                      ? Colors.green
                      : Colors.red,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  List<Transaction> _getMockTransactions() {
    return [
      Transaction(
        id: '1',
        title: 'Salary',
        amount: 5000,
        date: DateTime.now(),
        type: TransactionType.income,
        icon: Icons.work,
      ),
      Transaction(
        id: '2',
        title: 'Rent',
        amount: 1500,
        date: DateTime.now(),
        type: TransactionType.expense,
        icon: Icons.home,
      ),
      Transaction(
        id: '3',
        title: 'Groceries',
        amount: 250,
        date: DateTime.now(),
        type: TransactionType.expense,
        icon: Icons.shopping_cart,
      ),
    ];
  }
}

class _FinancialSummaryItem extends StatelessWidget {
  final String title;
  final String amount;

  const _FinancialSummaryItem({required this.title, required this.amount});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(title, style: Theme.of(context).textTheme.bodyMedium),
        const SizedBox(height: 4),
        Text(amount, style: Theme.of(context).textTheme.titleLarge),
      ],
    );
  }
}
